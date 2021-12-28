package eu.schnuff.bonfo2.filewrapper

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import java.io.File

interface FileWrapper {
    val uri: Uri
    val name: String
    val lastModified: Long
    val isDirectory: Boolean
    val isFile: Boolean
    val length: Long

    fun createFile(mimeType: String, filename: String): FileWrapper
    fun getChild(filename: String): FileWrapper?
    fun listFiles(lastModified: Long=-1): Collection<FileWrapper>
    fun delete()

    

    companion object {
        fun getMediaStoreVersion(context: Context) = MediaStoreFileWrapper.getVersion(context)
        fun getMediaStoreGeneration(context: Context) = MediaStoreFileWrapper.getGeneration(context)

        fun fromUri(context: Context, uri: Uri) : FileWrapper {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                documentFileFromUri(context, uri)
            } else
                when (uri.scheme) {
                    ContentResolver.SCHEME_FILE, null -> OSFileWrapper(context, File(uri.path!!))
                    ContentResolver.SCHEME_CONTENT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        documentFileFromUri(context, uri)
                    } else {
                        TODO("VERSION.SDK_INT < LOLLIPOP")
                    }
                    else -> throw IllegalArgumentException("uri '$uri', scheme '${uri.scheme}' is not supported.")
                }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        fun mediaStore(context: Context) : FileWrapper {
            return MediaStoreFileWrapper(context)
        }

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        private fun documentFileFromUri(context: Context, uri: Uri) : FileWrapper {
            return DocumentFileWrapper(
                context,
                if(DocumentFile.isDocumentUri(context, uri)) {
                    DocumentFile.fromSingleUri(context, uri)
                } else {
                    DocumentFile.fromTreeUri(context, uri)
                }!!
            )
        }
    }
}