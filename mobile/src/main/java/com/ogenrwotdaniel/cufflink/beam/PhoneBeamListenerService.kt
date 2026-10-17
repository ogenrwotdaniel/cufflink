package com.ogenrwotdaniel.cufflink.beam

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ogenrwotdaniel.cufflink.R
import com.ogenrwotdaniel.cufflink.shared.BeamListenerService
import com.ogenrwotdaniel.cufflink.shared.ReceivedBeam
import java.io.File
import java.io.IOException

/**
 * Files sent back from the watch. Written to cache by the shared receiver, then
 * published to Downloads/Cufflink where any phone app can find them.
 */
class PhoneBeamListenerService : BeamListenerService() {

    override fun inboxFolder(): File = File(cacheDir, "incoming")

    override suspend fun onBeamArrived(beam: ReceivedBeam) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, beam.file.name)
                put(MediaStore.Downloads.MIME_TYPE, beam.header.mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Cufflink/")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Downloads refused ${beam.file.name}")
            contentResolver.openOutputStream(uri)!!.use { out -> beam.file.inputStream().use { it.copyTo(out) } }
            contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
            BeamQueue.arrived(Arrival(beam.file.name, uri, beam.file.length()))
            notify(beam.file.name, "Saved to Downloads/Cufflink", Intent(Intent.ACTION_VIEW).setDataAndType(uri, beam.header.mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } finally {
            beam.file.delete()
        }
    }

    override fun onBeamFailed(error: Throwable) {
        notify("Watch file didn't arrive", error.message ?: "Transfer interrupted", null)
    }

    private fun notify(title: String, text: String, open: Intent?) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Files from your watch", NotificationManager.IMPORTANCE_DEFAULT),
        )
        val tap = open?.let {
            PendingIntent.getActivity(this, title.hashCode(), Intent.createChooser(it, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        }
        val n = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_beam)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(this).notify(title.hashCode(), n)
    }

    private companion object {
        const val CHANNEL = "from_watch"
    }
}
