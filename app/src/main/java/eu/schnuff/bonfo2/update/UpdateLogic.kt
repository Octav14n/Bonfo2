package eu.schnuff.bonfo2.update

import android.content.Context
import android.util.Log
import eu.schnuff.bonfo2.data.AppDatabase
import eu.schnuff.bonfo2.data.ePubItem.EPubItem
import eu.schnuff.bonfo2.data.ePubItem.EPubItemDAO
import eu.schnuff.bonfo2.helper.Setting
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.BufferedInputStream
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory
import kotlin.concurrent.thread

object UpdateLogic {
    private const val LARGE_FILE_MIN_SIZE: Long = (1024 shl 1) * 100
    private const val CONSUMER_COUNT = 4
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
        val dao = AppDatabase.getDatabase(context).ePubItemDao()
        val directories = Setting(context).watchedDirectories
        val queue = PriorityBlockingQueue<File>(200, compareByDescending(File::lastModified))

        val supplier = thread {
            directories.forEach {
                File(it).walk().forEach {
                    if (it.extension == "epub")
                        queue.add(it)
                }
            }
        }

        val others = ConcurrentHashMap(dao.getAllNow().associateBy { it.filePath }.toMutableMap())

        val idx = AtomicInteger(0)

        val runnable = Runnable {
            while (supplier.isAlive || queue.isNotEmpty()) {
                queue.poll(100, TimeUnit.MILLISECONDS)?.let {
                    val i = idx.incrementAndGet()
                    onProgress(queue.size + i, i)
                    try {
                        readEPub(it, others[it.absolutePath], dao)?.run {
                            others.remove(it.absolutePath)
                        }
                    } catch (e: Exception) {
                        Log.w("update", "Epub Error (${it.absolutePath}):", e)
                    }
                }
            }
        }
        val threads = Array(CONSUMER_COUNT) {
            Thread(runnable).also {
                it.start()
            }
        }

        threads.forEach {
            it.join()
        }

        dao.delete(others.values)
        onComplete()
    }

    private fun readEPub(file: File, other: EPubItem?, dao: EPubItemDAO): EPubItem? {
        if (other != null && Date(file.lastModified()) == other.modified && file.length() == other.fileSize)
            return other

        ZipFile(file).use { epub ->
            val opfEntry = findEntry(epub, OPF_EXTENSION, XPATH_OPF_FILE_PATH) ?: return null
            if (opfEntry.crc == other?.opfCrc) {
                // The cached file information does not have to be updated
                val modified = Date(file.lastModified())
                if (modified != other.modified) {
                    val item = other.copy(modified = modified)
                    dao.update(item)
                    return item
                }
                return other
            }
            // Log.d("reading", "Now reading " + file.nameWithoutExtension)
            // Log.d("reading-time", System.nanoTime().toString() + " - open opf")
            BufferedInputStream(epub.getInputStream(opfEntry)).use { opfStream ->
                // Log.d("reading-time", System.nanoTime().toString() + " - read opf")
                val opf = DOCUMENT_FACTORY.newDocumentBuilder().parse(opfStream)
                opfStream.close()
                // Log.d("reading-time", System.nanoTime().toString() + " - normalize opf")
                opf.normalizeDocument()

                // Log.d("reading-time", System.nanoTime().toString() + " - grab meta")
                val meta  = XPATH_META.evaluate(opf, XPathConstants.NODE) as Node
                meta.parentNode.removeChild(meta)

                // Log.d("reading-time", System.nanoTime().toString() + " - grab meta children")
                val id = path(meta, XPATH_ID) ?: "file://" + file.canonicalPath
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
                    val titlePage = epub.getEntry(titlePageHref)

                    if (titlePage != null) {
                        epub.getInputStream(titlePage).reader().use {
                            val titlePageText = it.readText()
                            it.close()

                            description =
                                Regex("<body[^>]*>(.+)</body", RegexOption.DOT_MATCHES_ALL).replace(titlePageText, "$1")
                                    .replace(Regex("<br[^>]*>"), "\n")
                                    .replace(Regex("(<[^>]*>)|(^\\W+)|(\\W+$)"), "")
                                    .replace(Regex("\n(\\s*\n)+"), "\n")

                            Log.d(this::class.simpleName, "\tNew description: $description")
                        }
                    } else
                        Log.d(this::class.simpleName, "\tNo title page found. (ID: $titlePageId, HREF: $titlePageHref [XPATH: $titlePageHrefXPath])")
                }

                // Log.d("reading-time", System.nanoTime().toString() + " - build EPubItem")

                // Log.d("reading-time", System.nanoTime().toString() + " - close zip file " + file.name)
                return createItem(other, dao, file.absolutePath, file.name, Date(file.lastModified()),
                    file.length(), opfEntry.crc, id, title, author, fandom, description, genres.toTypedArray(),
                    characters.toTypedArray())
            }
        }
    }

    private fun findEntry(epub: ZipFile, extension: String, xPath: XPathExpression) : ZipEntry? {
        val ret = epub.entries().asSequence().first { !it.isDirectory && it.name.endsWith(extension) }
        if (ret != null) return ret

        val containerInfoEntry = epub.getEntry(CONTAINER_FILE_PATH)!!
        val opfPath = BufferedInputStream(epub.getInputStream(containerInfoEntry)).use {
            val container = DOCUMENT_FACTORY.newDocumentBuilder().parse(it)
            it.close()
            xPath.evaluate(container, XPathConstants.STRING).toString()
        }
        if (opfPath.isNotEmpty()) return epub.getEntry(opfPath)
        return null
    }

    private fun createItem(other: EPubItem?, dao: EPubItemDAO, filePath: String, filename: String,
                           dateLastModified: Date, length: Long, crc: Long, id: String, title: String, author: String?,
                           fandom: String?, description: String?, genres: Array<String>,
                           characters: Array<String>): EPubItem {
        val item = EPubItem(
            filePath, filename, dateLastModified, length, crc,
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