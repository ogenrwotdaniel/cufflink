package com.ogenrwotdaniel.cufflink.wear.beam

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ogenrwotdaniel.cufflink.wear.MainActivity
import com.ogenrwotdaniel.cufflink.wear.R

/** "Something landed in your Pocket" — the watch's answer to a beam. */
object BeamNotifications {
    private const val CHANNEL = "beams"

    fun arrived(context: Context, fileName: String, text: String) {
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_POCKET, fileName)
        val pending = PendingIntent.getActivity(
            context, fileName.hashCode(), open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        post(context, fileName.hashCode(), fileName, text, pending)
    }

    fun failed(context: Context, reason: String) {
        post(context, 1, "Beam didn't land", reason, null)
    }

    private fun post(context: Context, id: Int, title: String, text: String, tap: PendingIntent?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.channel_beams), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_beam)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
