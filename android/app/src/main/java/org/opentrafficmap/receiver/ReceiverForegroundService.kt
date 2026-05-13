package org.opentrafficmap.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class ReceiverForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var tapIntent: PendingIntent? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW)
                .apply { description = getString(R.string.notif_channel_desc) }
        )

        tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )

        startForeground(NOTIF_ID, buildNotification(getString(R.string.notif_running)))

        val pm = getSystemService(PowerManager::class.java)
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "v2x2map:receiver")
            .also { it.acquire(MAX_SESSION_MS) }

        instance = this
        return START_STICKY
    }

    override fun onDestroy() {
        instance = null
        try { wakeLock?.release() } catch (_: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    fun updateStats(totalPackets: Int, ratePerMin: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, buildNotification(getString(R.string.notif_stats, totalPackets, ratePerMin)))
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(tapIntent)
            .build()

    companion object {
        private const val CHANNEL_ID     = "v2x2map_receiver"
        private const val NOTIF_ID       = 1
        private const val MAX_SESSION_MS = 8L * 60 * 60 * 1000

        @Volatile var instance: ReceiverForegroundService? = null
    }
}
