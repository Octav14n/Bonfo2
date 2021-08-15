package eu.schnuff.bonfo2.filewrapper

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File

class OSFileWrapper(
    private val context: Context,
    private val file: File,
    name: String? = null,
    lastModified: Long? = null,
    mimeType: String? = null,
    size: Long? = null
): FileWrapper {
    override val uri: Uri = file.toUri()
    override val name: String = name ?: file.name
    override val isDirectory: Boolean
    override val isFile: Boolean
    override val lastModified: Long = lastModified ?: file.lastModified()
    override val length: Long = size?: file.length()

    init {
        when (mimeType) {
            null -> {
                isDirectory = file.isDirectory
                isFile = file.isFile
            }
            DocumentsContract.Document.MIME_TYPE_DIR -> {
                isDirectory = true
                isFile = false
            }
            else -> {
                isDirectory = false
                isFile = true
            }
        }
    }

    override fun createFile(mimeType:String, filename: String): FileWrapper {
        return OSFileWrapper(context, File(file, filename).apply {
            createNewFile()
        })
    }

    override fun delete() {
        file.delete()
    }

    override fun getChild(filename: String): FileWrapper? {
        val f = File(file, filename)
        if (f.exists())
            return OSFileWrapper(context, f)
        return null
    }

    override fun listFiles(_lastModified: Long) : Collection<FileWrapper> {
        if (!isDirectory) return emptyList()
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
            // Try using native (fast) FileIO api on versions before R.
            return file.listFiles()?.map{ OSFileWrapper(context, it) } ?: emptyList()
        }

        // If we have to use SAF then we will try to speed it up at least somewhat.
        //val childs = mutableListOf<FileWrapper>()
        val childs = file.listFiles()?.map { OSFileWrapper(context, it) } ?: emptyList()
        /*DocumentFileWrapper.queryDocumentTree(context, uri) { originalFileName, childId, lastModified, mimeType, size ->
            if (_lastModified < lastModified)
                return@queryDocumentTree
            childs.add(
                OSFileWrapper(
                    context,
                    File(file, originalFileName),
                    originalFileName,
                    lastModified,
                    mimeType,
                    size
                )
            )
        }*/
        return childs
    }
}