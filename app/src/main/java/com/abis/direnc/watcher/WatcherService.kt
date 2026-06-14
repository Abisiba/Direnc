package com.abis.direnc.watcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Oturum boyunca canlı kalan foreground servis. Kilit açılışını yakalar ve gerekirse
 * ritüel overlay'ini başlatır; ekran kapanınca "bu açılışta geçildi" durumunu sıfırlar.
 */
class WatcherService : Service() {

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    WatcherState.passedThisUnlock = false
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (UnlockGate.shouldShowRitual(
                            WatcherState.sessionActive,
                            WatcherState.passedThisUnlock,
                        )
                    ) {
                        RitualOverlayActivity.launch(this@WatcherService)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        WatcherState.sessionActive = true
        startForeground(NOTIF_ID, buildNotification())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        WatcherState.sessionActive = false
        WatcherState.passedThisUnlock = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Çalışma oturumu",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Gözcü aktifken görünür" }
            mgr.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Direnç — oturum aktif")
            .setContentText("Çalışıyorsun. Gözcü açık.")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "direnc_watcher"
        private const val NOTIF_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, WatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, WatcherService::class.java))
        }
    }
}
