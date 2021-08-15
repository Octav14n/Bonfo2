package eu.schnuff.bonfo2.filewrapper

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build.VERSION_CODES.KITKAT
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import kotlin.concurrent.thread

const val UPDATE_CACHE_ON_ERROR_INTERVAL = 25000L

@RequiresApi(LOLLIPOP)
class DocumentFileWrapper(
    private val context: Context,
    private val file: DocumentFile,
    private var _fileName: String? = null,
    private var _lastModified: Long? = null,
    private var _mimeType: String? = null,
    private var _size: Long? = null
): FileWrapper {
    override val uri: Uri
        get() = file.uri

    override val name: String
        get() {
            if (_fileName == null)
                _fileName = file.name ?: "NAN"
            return _fileName!!
        }
    override val isDirectory: Boolean
        get() = _mimeType == DocumentsContract.Document.MIME_TYPE_DIR || file.isDirectory
    override val isFile: Boolean
        get() = file.isFile

    override val lastModified: Long
        get() {
            if (_lastModified == null)
                _lastModified = file.lastModified()
            return _lastModified!!
        }
    override val length: Long
        get()  {
            if (_size == null)
                _size = file.length()
            return _size!!
        }

    override fun createFile(mimeType: String, filename: String): FileWrapper {
        var f: DocumentFileWrapper? = null
        while (f == null) {
            f = try {
                file.createFile(mimeType, filename)?.run {
                    DocumentFileWrapper(context, this)
                }
            } catch (e: Throwable) {
                Log.d(this::class.simpleName, "Failed creating file: %s [mime-type: %s]".format(filename, mimeType), e)
                null
            }
        }
        return f
    }

    override fun delete() {
        file.delete()

    }

    override fun getChild(filename: String): DocumentFileWrapper? {
        queryDocumentTree(context, uri) { originalFileName, childId, lastModified, mimeType, size ->
            if (originalFileName == filename) {
                val df = DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(uri, childId))
                if (df != null)
                    return DocumentFileWrapper(context, df, filename, lastModified, mimeType, size)
                return null
            }
        }

        return null
    }

    override fun listFiles(lastModified: Long) : Collection<FileWrapper> {
        if (!isDirectory) return emptyList()
        val childs = mutableListOf<FileWrapper>()
        queryDocumentTree(context, uri) { originalFileName, childId, _lastModified, mimeType, size ->
            if (_lastModified < lastModified)
                return@queryDocumentTree
            DocumentFile.fromSingleUri(context, DocumentsContract.buildDocumentUriUsingTree(uri, childId))?.also {
                childs.add(DocumentFileWrapper(
                    context,
                    it,
                    originalFileName,
                    lastModified,
                    mimeType,
                    size
                ))
            }
        }
        return childs
    }

    companion object {
        internal inline fun queryDocumentTree(context: Context, uri: Uri, itterateChilds: (filename: String, childId: String, lastModified: Long, mimeType: String, size: Long) -> Unit) {
            val baseUri = if (uri.scheme == ContentResolver.SCHEME_FILE) {
                TODO("convert uri to childDocumentsUri")
            } else {
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    uri,
                    DocumentsContract.getTreeDocumentId(uri)
                )
            }

            context.contentResolver.query(
                baseUri, arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE,
                ), null, emptyArray(), DocumentsContract.Document.COLUMN_LAST_MODIFIED
            )?.use {
                val nameIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val idIndex = it.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val idxLastModified = it.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                val idxMimeType = it.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val idxSize = it.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val childId = it.getString(idIndex)
                    val lastModified = it.getLong(idxLastModified)
                    val mimeType = it.getString(idxMimeType)
                    val size = it.getLong(idxSize)
                    itterateChilds(name, childId, lastModified, mimeType, size)
                }

                it.close()
            }
        }
    }
}