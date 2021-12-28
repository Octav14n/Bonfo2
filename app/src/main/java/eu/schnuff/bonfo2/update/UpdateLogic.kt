package eu.schnuff.bonfo2.update

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import eu.schnuff.bonfo2.data.AppDatabase
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.data.ePubItem.EPubItemDAO
import eu.schnuff.bonfo2.filewrapper.FileWrapper
import eu.schnuff.bonfo2.helper.Setting
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory

const val DESCRIPTION_MAX_LINES = 30
const val DESCRIPTION_REDUCE_TO_LINES = 15
const val DESCRIPTION_MAX_CHARACTERS = DESCRIPTION_MAX_LINES * 10000
const val DESCRIPTION_REDUCE_TO_CHARACTERS = DESCRIPTION_MAX_CHARACTERS / 2

object UpdateLogic {
    private const val LARGE_FILE_MIN_SIZE: Long = (1024 shl 1) * 100
    private const val CONSUMER_COUNT = 16
    private val CONSUMER_ACTIVE_READING = Semaphore(CONSUMER_COUNT)
    private const val CONSUMER_OUT_OF_MEM_RETRIES = 3
    private const val CONTAINER_FILE_PATH = "META-INF/container.xml"
    private const val OPF_EXTENSION = ".opf"
    private val XPATH = XPathFactory.newInstance().newXPath()
    private val XPATH_META = XPATH.compile("/package/metadata")
    private val XPATH_ID = XPATH.compile("identifier/text()")
    private val XPATH_TITLE = XPATH.compile("title/text()")
    private val XPATH_AUTHOR = XPATH.compile("creator/text()")
    private val XPATH_FANDOM = XPATH.compile("meta[@name='fandom']/@content")
    private val XPATH_DESCRIPTION = XPATH.compile("description/text()")
    private val XPATH_GENRES = XPATH.compile("type/text()|subject[not(starts-with(., 'Last Update') or text() = 'FanFiction')]/text()")
    private val XPATH_CHARACTERS = XPATH.compile("meta[@name='characters']/@content")
    private val XPATH_OPF_FILE_PATH = XPATH.compile("//rootfile/@full-path[1]")
    private val XPATH_FIRST_PAGE_ID = XPATH.compile("//spine/itemref[1]/@idref")
    private const val XPATH_FIRST_PAGE_HREF = "//manifest/item[@id='%s']/@href"
    private val DOCUMENT_FACTORY = DocumentBuilderFactory.newInstance()
    private const val UPDATE_INTERVAL_MILLIS = 250

    fun readItems(context: Context, onComplete: () -> Unit = {}, onProgress: (maxElements: Int, finishedElements: Int) -> Unit = { _, _ -> }) {
        val setting = Setting(context)
        val dao = AppDatabase.getDatabase(context).ePubItemDao()

        val others: MutableMap<String, EPubItem> =
            dao.getAllNow().associateBy { it.filePath }.toMutableMap()
        val lastModified = setting.lastModified
        Log.d(this::class.simpleName, "Only using files newer than $lastModified")

        val total = AtomicInteger(0)
        var useMediaStore = false

        runBlocking {
            flow {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q && setting.useMediaStore) {
                    Log.d(this::class.simpleName, "using MediaStore.")
                    useMediaStore = true
                    val directory = FileWrapper.mediaStore(context)
                    val files = if (setting.lastMediaStoreVersion != FileWrapper.getMediaStoreVersion(context)) {
                        directory.listFiles(lastModified)
                    } else {
                        directory.listFiles()
                    }
                    total.set(files.size)
                    files.forEach { emit(it) }
                } else {
                    val reason = when {
                        setting.useMediaStore -> "it is deactivated in settings."
                        Build.VERSION.SDK_INT > Build.VERSION_CODES.R -> "your android is too old ${Build.VERSION.SDK_INT} <= ${Build.VERSION_CODES.Q}"
                        else -> "Reason: unknown."
                    }
                    Log.d(this::class.simpleName, "not using MediaStore. because $reason")
                }
                readDirectories(context, this, total, lastModified, others)
            }.catch { t ->
                Log.w("update", "error:", t)
            }.onCompletion {
                Log.i("update", "completed.")
            }
                //.flowOn(Dispatchers.IO)
            .buffer()
            .collectIndexed { idx, it ->
                onProgress(total.get(), idx)
                try {
                    readEPub(context, it, others[it.uri.toString()], dao)?.run {
                        others.remove(it.uri.toString())
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e("update", "out of memory '${it.uri}' [${it.name} / ${it.length / (1024 shl 2)}M]", e)
                    insertErrorEPubItem(dao, others[it.uri.toString()], it, e)
                    others.remove(it.uri.toString())
                } catch (e: FileNotFoundException) {
                    Log.e("update", "file not found ${it.uri}", e)
                    insertErrorEPubItem(dao, others[it.uri.toString()], it, e)
                    others.remove(it.uri.toString())
                } catch (e: Exception) {
                    Log.w("update", "Epub Error (${it.uri}):", e)
                    insertErrorEPubItem(dao, others[it.uri.toString()], it, e)
                    others.remove(it.uri.toString())
                }
            }
        }
        Log.i("update", "update finished.")
        Looper.prepare()
        Toast.makeText(context, "Updated ${total.get()} EBooks.", Toast.LENGTH_SHORT).show()

        if (lastModified <= 0)
            dao.delete(others.values)
        onComplete()
        setting.lastModified = dao.getLastModified()
    }

    private suspend fun readDirectories(
        context: Context,
        flow: FlowCollector<FileWrapper>,
        total: AtomicInteger,
        lastModified: Long,
        others: Map<String, EPubItem>
    ) {
        val directories = Setting(context).watchedDirectories

        directories.map {
            val directory = FileWrapper.fromUri(context, it.toUri())
            readDirectoryToQueue(flow, total, lastModified, directory) { uri ->
                others.containsKey(uri.toString())
            }
        }
    }

    private fun insertErrorEPubItem(dao: EPubItemDAO, other: EPubItem?, file: FileWrapper, e: Throwable) {
        createItem(
            other,
            dao,
            file.uri,
            file.name,
            Date(file.lastModified),
            file.length,
            0,
            file.uri.toString(),
            file.name + " [ERROR]",
            "ERROR",
            null,
            e.localizedMessage,
            emptyArray(),
            emptyArray()
        )
    }

    private suspend fun readDirectoryToQueue(
        flow: FlowCollector<FileWrapper>,
        total: AtomicInteger,
        lastModified: Long,
        dir: FileWrapper,
        isRead: (it: Uri) -> Boolean
    ) {
        //Log.d(this::class.simpleName, "Now starting reading '%s' to queue".format(dir.uri))

        val files = dir.listFiles(lastModified)
        //Log.d(this::class.simpleName, "\tFound files: %s [size: %d]".format(files, files.size))

        files.forEach {
            //Log.d(this::class.simpleName, "\tFound file %s".format(it.uri))
            when {
                isRead(it.uri) -> {}
                it.isDirectory -> readDirectoryToQueue(flow, total, lastModified, it, isRead)
                it.isFile && it.name.endsWith(".epub", true) -> {
                    total.incrementAndGet()
                    flow.emit(it)
                }
                else -> {}
            }
        }
    }

    private suspend fun readEPub(context: Context, file: FileWrapper, other: EPubItem?, dao: EPubItemDAO): EPubItem? {
        if (other != null && Date(file.lastModified) == other.modified && file.length == other.fileSize)
            return other

        CONSUMER_ACTIVE_READING.acquire()
        try {
            val v = withContext(Dispatchers.IO) {
                when (file.uri.scheme) {
                    ContentResolver.SCHEME_FILE -> readEntryFromFileUri(context, file, other, dao)
                    else -> {
                        val runtime = Runtime.getRuntime()
                        var tries = CONSUMER_OUT_OF_MEM_RETRIES
                        if (file.length >= runtime.maxMemory())
                            throw OutOfMemoryError("Not enough space to read in ${file.name} [${file.length / 1000 / 1000}MB")
                        while (file.length >= runtime.freeMemory()) {
                            delay(10)
                            runtime.gc()
                            tries -= 1
                            if (tries <= 0)
                                throw OutOfMemoryError("Not enough space to read in ${file.name} [${file.length / 1000 / 1000}MB")
                        }

                        readEntryFromContentUri(context, file, other, dao)
                    }
                }
            }
            return v
        } finally {
            CONSUMER_ACTIVE_READING.release()
        }
    }

    private fun readEntryFromFileUri(context: Context, file: FileWrapper, other: EPubItem?, dao: EPubItemDAO) : EPubItem?{
        val zip = ZipFile(file.uri.toFile())
        val opfEntry = zip.entries().toList().first { it.name.endsWith(OPF_EXTENSION, true) }

        zip.use {
            return readEntry(
                opfEntry,
                zip.getInputStream(opfEntry).readBytes(),
                other,
                dao,
                file
            ) {
                zip.getInputStream(zip.getEntry(it)).readBytes()
            }
        }
    }

    private fun readEntryFromContentUri(context: Context, file: FileWrapper, other: EPubItem?, dao: EPubItemDAO) : EPubItem?{
        val fileStream = context.contentResolver.openInputStream(file.uri)!!
        val zipStream = ZipInputStream(fileStream)
        var entry: ZipEntry? = zipStream.nextEntry

        // Cache file
        try {
            val zipMap = HashMap<ZipEntry, ByteArray>()
            while (entry != null) {
                val data = zipStream.readBytes()
                zipMap[entry] = data
                zipStream.closeEntry()

                if (entry.name.endsWith(OPF_EXTENSION, true)) {
                    return readEntry(entry, data, other, dao, file) {
                        if (zipMap.keys.any { k -> k.name == it })
                            return@readEntry zipMap[zipMap.keys.first { k -> k.name == it }]

                        var entry2 = entry
                        while (entry2 != null) {
                            val data2 = zipStream.readBytes()
                            zipMap[entry2] = data2
                            zipStream.closeEntry()

                            if (entry2.name == it)
                                return@readEntry data2

                            entry2 = zipStream.nextEntry
                        }

                        return@readEntry null
                    }
                }

                entry = zipStream.nextEntry
            }
        } finally {
            zipStream.close()
            fileStream?.close()
        }
        return null
    }

    private fun readEntry(
        opfEntry: ZipEntry,
        data: ByteArray,
        other: EPubItem?,
        dao: EPubItemDAO,
        file: FileWrapper,
        getEntry: (uri: String) -> ByteArray?
    ): EPubItem {
        //val opfEntry = findEntry(zipStream, OPF_EXTENSION, XPATH_OPF_FILE_PATH) ?: return null
        if (opfEntry.crc == other?.opfCrc) {
            // The cached file information does not have to be updated
            val modified = Date(file.lastModified)
            if (modified != other.modified) {
                val item = other.copy(modified = modified)
                dao.update(item)
                return item
            }
            return other
        }
        // Log.d("reading", "Now reading " + file.nameWithoutExtension)
        // Log.d("reading-time", System.nanoTime().toString() + " - open opf")

        val opf = DOCUMENT_FACTORY.newDocumentBuilder().parse(ByteArrayInputStream(data))
        // Log.d("reading-time", System.nanoTime().toString() + " - normalize opf")
        opf.normalizeDocument()

        // Log.d("reading-time", System.nanoTime().toString() + " - grab meta")
        val meta = XPATH_META.evaluate(opf, XPathConstants.NODE) as Node
        meta.parentNode.removeChild(meta)

        // Log.d("reading-time", System.nanoTime().toString() + " - grab meta children")
        val id = path(meta, XPATH_ID) ?: file.uri.toString()
        val title = path(meta, XPATH_TITLE) ?: "<No Title>"
        val author = path(meta, XPATH_AUTHOR)
        val fandom = path(meta, XPATH_FANDOM)
        var description = path(meta, XPATH_DESCRIPTION, "\n\n")
        val genres = path(meta, XPATH_GENRES)?.split(", ") ?: Collections.emptyList<String>()
        val characters = path(meta, XPATH_CHARACTERS)?.split(", ") ?: Collections.emptyList<String>()

        if (fandom.isNullOrEmpty() && description.isNullOrBlank() && genres.isNullOrEmpty()) {
            Log.d(this::class.simpleName, "${file.name} empty description, read title page")
            val titlePageId = XPATH_FIRST_PAGE_ID.evaluate(opf)
            val titlePageHrefXPath = XPATH_FIRST_PAGE_HREF.format(titlePageId.replace("'", "\\'"))
            val opfDir = opfEntry.name.replaceAfterLast('/', "", "")
            val titlePageHref = opfDir + XPATH.evaluate(titlePageHrefXPath, opf)
            val titlePage = getEntry(titlePageHref)

            if (titlePage != null) {
                val titlePageText = String(titlePage)

                description =
                    titlePageText.replace(Regex(".*<body>(.+)</body.*", RegexOption.DOT_MATCHES_ALL), "$1")
                        .replace(Regex("<br[^>]*>"), "\n")
                        .replace(Regex("(<[^>]*>)|(^\\W+)|(\\W+$)"), "")
                        .replace(Regex("\n(\\s*\n)+"), "\n")

                val descriptionLines = description.lines()
                if (descriptionLines.size > DESCRIPTION_MAX_LINES)
                    description = descriptionLines.subList(0, DESCRIPTION_REDUCE_TO_LINES).joinToString("\n")
                if (description.length > DESCRIPTION_MAX_CHARACTERS)
                    description = description.substring(0, DESCRIPTION_REDUCE_TO_CHARACTERS)


                Log.d(this::class.simpleName, "\tNew description: $description")
            } else
                Log.d(
                    this::class.simpleName,
                    "\tNo title page found. (ID: $titlePageId, HREF: $titlePageHref [XPATH: $titlePageHrefXPath])"
                )
        }
        // Log.d("reading-time", System.nanoTime().toString() + " - build EPubItem")

        // Log.d("reading-time", System.nanoTime().toString() + " - close zip file " + file.name)
        return createItem(
            other, dao, file.uri, file.name, Date(file.lastModified),
            file.length, opfEntry.crc, id, title, author, fandom, description, genres.toTypedArray(),
            characters.toTypedArray()
        )
    }

    private fun findEntry(epub: ZipInputStream, extension: String, xPath: XPathExpression) : ZipEntry? {
        epub.reset()
        var entry: ZipEntry? = epub.nextEntry
        while (entry != null) {
            if (!entry.isDirectory && entry.name.endsWith(extension, true))
                return entry

            entry = epub.nextEntry
        }

        if (openEntry(epub, CONTAINER_FILE_PATH) != null) {
            val opfPath = BufferedInputStream(epub).use {
                val container = DOCUMENT_FACTORY.newDocumentBuilder().parse(it)
                it.close()
                xPath.evaluate(container, XPathConstants.STRING).toString()
            }

            if (opfPath.isNotEmpty())
                return openEntry(epub, opfPath)
        }

        return null
    }

    private fun openEntry(epub: ZipInputStream, name: String): ZipEntry? {
        epub.reset()
        var entry: ZipEntry? = epub.nextEntry
        while (entry != null) {
            if (entry.name == name)
                return entry

            entry = epub.nextEntry
        }
        return null
    }

    private fun createItem(other: EPubItem?, dao: EPubItemDAO, filePath: Uri, filename: String,
                           dateLastModified: Date, length: Long, crc: Long, id: String, title: String, author: String?,
                           fandom: String?, description: String?, genres: Array<String>,
                           characters: Array<String>): EPubItem {
        val item = EPubItem(
            filePath.toString(), filename, dateLastModified, length, crc,
            id, title, author, fandom, description, genres, characters
        )
        // Log.d("reading-time", System.nanoTime().toString() + " - Save EPubItem")
        if (other != null) {
            dao.update(item)
        } else {
            dao.insert(item)
        }
        return item
    }

    private fun path(node: Node, xPath: XPathExpression, seperator: String = ", "): String? {
        val texts = xPath.evaluate(node, XPathConstants.NODESET) as NodeList
        return if (texts.length == 0) {
            null
        } else {
            Array(texts.length) { i -> texts.item(i).nodeValue.trim() }.joinToString(seperator)
        }
    }
}