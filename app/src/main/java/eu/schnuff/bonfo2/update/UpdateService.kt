package eu.schnuff.bonfo2.update

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import eu.schnuff.bonfo2.MainActivity
import eu.schnuff.bonfo2.R
import kotlin.concurrent.thread

class UpdateService : LifecycleService() {
    private val binder = UpdateBinder(this)
    private val onStopCallback = mutableListOf<() -> Unit>()

    private val _progressMax = MutableLiveData(0)
    private val _progressNow = MutableLiveData(0)
    private val _progressing = MutableLiveData(false)

    val progressMax: LiveData<Int> = _progressMax
    val progressNow: LiveData<Int> = _progressNow
    val progressing: LiveData<Boolean> = _progressing

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> thread {
                _progressing.postValue(true)
                val notification = createNotification()
                startForegroundService(notification)
                UpdateLogic.readItems(this,
                    onProgress = { max, now -> startForegroundService(notification, now, max) },
                    onComplete = { this.stopForegroundService(startId) }
                )
            }
            ACTION_ABORT -> stopForegroundService(startId)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    /* Used to build and start foreground service. */
    private fun startForegroundService(notification: NotificationCompat.Builder, progress:Int? = null, maxProgress:Int? = null) {
        // Start foreground service.
        if (progress != null)
            _progressNow.postValue(progress)
        if (maxProgress != null)
            _progressMax.postValue(maxProgress)

        notification.apply {
            if (progress != null && maxProgress != null) {
                setProgress(maxProgress, progress, false)
                setContentText("$progress/$maxProgress")
            } else {
                setProgress(1, 0, true)
            }
        }
        startForeground(CHANNEL_NR, notification.build())
    }

    private fun createNotification() : NotificationCompat.Builder {
        createNotificationChannel()
        // Create notification default intent.
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, if(VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val abortIntent = PendingIntent.getService(this, 0, Intent(this, this::class.java)
                .setAction(ACTION_ABORT), if(VERSION.SDK_INT >= VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)

        // Create notification builder.
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.EpubRefreshTitle))
                .setContentText(getString(R.string.EPubRefreshInitializing))
                .setSmallIcon(R.drawable.ic_sync)
                .addAction(R.drawable.ic_warning, getString(R.string.EPubRefreshActionAbort), abortIntent)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_PROGRESS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.priority = NotificationManager.IMPORTANCE_LOW
        }
        return builder
    }

    private fun stopForegroundService(startId: Int) {
        // Stop foreground service and remove the notification.
        _progressing.postValue(false)

        stopForeground(true)
        // Stop the foreground service.
        stopSelf(startId)
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.channel_name),
                    NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    class UpdateBinder(val Service: UpdateService) : Binder()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_ABORT = "ACTION_ABORT"
        const val CHANNEL_ID = "Epub Refresh"
        const val CHANNEL_NR = 1

    }
}
