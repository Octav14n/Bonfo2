package eu.schnuff.bonfo2.filewrapper

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.io.File

@RequiresApi(Build.VERSION_CODES.R)
private const val VOLUME = MediaStore.VOLUME_EXTERNAL

@RequiresApi(Build.VERSION_CODES.R)
const val MODIFIED_COLUMN = MediaStore.Files.FileColumns.DATE_MODIFIED
@RequiresApi(Build.VERSION_CODES.R)
const val GENERATION_COLUMN = MediaStore.Files.FileColumns.GENERATION_MODIFIED

@RequiresApi(Build.VERSION_CODES.R)
class MediaStoreFileWrapper(
    private val context: Context
) : FileWrapper {
    override val uri: Uri = MediaStore.Files.getContentUri(VOLUME)
    override val name: String
        get() = MediaStore.AUTHORITY
    override val lastModified: Long
        get() = 0
    override val isDirectory: Boolean
        get() = true
    override val isFile: Boolean
        get() = false
    override val length: Long
        get() = 0

    override fun createFile(mimeType: String, filename: String): FileWrapper {
        TODO("Not yet implemented")
    }

    override fun getChild(filename: String): FileWrapper? {
        TODO("Not yet implemented")
    }

    override fun listFiles(lastModified: Long): Collection<FileWrapper> {
        Log.d(this::class.simpleName, "listing files newer than $lastModified, using $MODIFIED_COLUMN DESC.")
        val files = mutableListOf<FileWrapper>()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("epub")
        val selections = mutableMapOf<String, String>()
        if (lastModified > 0)
            selections["$GENERATION_COLUMN > ?"] = lastModified.toString()

        if(mimeType != null)
            selections["${MediaStore.Files.FileColumns.MIME_TYPE} = ?"] = mimeType

        val keys = selections.keys

        context.contentResolver.query(
            uri,
            arrayOf(
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA,
                MODIFIED_COLUMN,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
            ),
            keys.joinToString(" AND "),
            keys.map { selections[it] }.toTypedArray(),
            "$MODIFIED_COLUMN DESC"
        )?.also {
            val idxName = it.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val idxDATA = it.getColumnIndex(MediaStore.Files.FileColumns.DATA)
            val idxModified = it.getColumnIndex(MODIFIED_COLUMN)
            val idxMimeType = it.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
            val idxSize = it.getColumnIndex(MediaStore.Files.FileColumns.SIZE)

            //Log.d(this::class.simpleName, "Now indexing.")
            while (it.moveToNext()) {
                val fileName = it.getString(idxName)
                if (mimeType == null && !fileName.endsWith(".epub", true))
                    continue

                //Log.d(this::class.simpleName, "Found file: " + fileName)
                files.add(DocumentFileWrapper(
                    context,
                    DocumentFile.fromFile(
                        File(it.getString(idxDATA))
                    ),
                    fileName,
                    it.getLong(idxModified),
                    it.getString(idxMimeType),
                    it.getLong(idxSize)
                ))
            }
        }?.close()
        return files
    }

    override fun delete() {
        TODO("Not yet implemented")
    }

    companion object {
        fun getVersion(context: Context) = MediaStore.getVersion(context, VOLUME)
        fun getGeneration(context: Context) = MediaStore.getGeneration(context, VOLUME)
    }
}