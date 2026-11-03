package com.example.localbeat.downloader

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import androidx.core.app.NotificationCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.localbeat.MainActivity
import com.example.localbeat.data.repository.DownloadRepositoryImpl
import kotlinx.coroutines.*

/** Direct user-started FGS: no JobScheduler quota dependency on Android 16. */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { DownloadRepositoryImpl.get(this) }
    private var runner: Job? = null
    @Volatile private var generation = 0
    private var wakeLock: PowerManager.WakeLock? = null
    override fun onBind(intent: Intent?) = null
    private fun notification(stage: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancel = PendingIntent.getService(this, 1, Intent(this, DownloadService::class.java).setAction("cancel"),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "downloads").setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("LocalBeat audio import").setContentText(stage).setContentIntent(open)
            .setOngoing(true).addAction(0, "Cancel queue", cancel).build()
    }
    override fun onCreate() {
        super.onCreate()
        wakeLock = getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LocalBeat:AudioImport")
            .apply { acquire(6 * 60 * 60 * 1000L) }
        if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel("downloads", "Audio imports", NotificationManager.IMPORTANCE_LOW))
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        generation++
        val type = if (Build.VERSION.SDK_INT >= 35)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            else if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
        if (Build.VERSION.SDK_INT >= 29) startForeground(42, notification("Preparing queue"), type)
        else startForeground(42, notification("Preparing queue"))
        if (intent?.action == "cancel") {
            scope.launch { repository.dao.interrupted().forEach { repository.cancelDownload(it.id) } }
        } else if (runner?.isActive != true) runner = scope.launch {
            while (isActive) {
                val observed = generation
                while (repository.runNext { stage ->
                    if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(this@DownloadService, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                        getSystemService(NotificationManager::class.java).notify(42, notification(stage))
                }) { }
                val finished = withContext(Dispatchers.Main) {
                    if (observed != generation) false
                    else { runner = null; stopSelf(); true }
                }
                if (finished) break
            }
        }
        return START_NOT_STICKY
    }
    override fun onTimeout(startId: Int, fgsType: Int) {
        scope.launch { repository.dao.interrupted().forEach { repository.cancelDownload(it.id) } }
        stopSelf()
    }
    override fun onDestroy() {
        scope.launch { repository.dao.interrupted().forEach { repository.cancelDownload(it.id) }; scope.cancel() }
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }
}
