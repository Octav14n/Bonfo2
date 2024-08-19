package eu.schnuff.bonfo2.helper

import android.Manifest
import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import eu.schnuff.bonfo2.R

fun Context.withFilePermission(onGranted: (it: PermissionGrantedResponse?) -> Unit) {
    onGranted(null)
    /*Dexter
        .withContext(this)
        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        .withListener(object: PermissionListener {
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) { onGranted(p0) }

            override fun onPermissionRationaleShouldBeShown(p0: PermissionRequest?, p1: PermissionToken?) {
                MaterialDialog(this@withFilePermission).show {
                    title(R.string.permission_file_title)
                    message(R.string.permission_file_msg)
                    positiveButton {
                        p1?.continuePermissionRequest()
                    }
                    negativeButton {
                        p1?.cancelPermissionRequest()
                    }
                }
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                MaterialDialog(this@withFilePermission).show {
                    title(R.string.permission_file_title)
                    message(R.string.permission_file_msg)
                    icon(R.drawable.ic_warning)
                }
            }

        }).check()*/
}