package com.example.dualplayer

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MusicService : Service() {
    companion object {
        const val CHANNEL_ID = "music_channel"
        const val NOTIF_ID = 2001

        const val ACTION_PLAY = "ACTION_PLAY"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_PREV = "ACTION_PREV"
        const val ACTION_NEXT = "ACTION_NEXT"
        const val ACTION_PLAY_FILE = "ACTION_PLAY_FILE"
    }

    private var player: ExoPlayer? = null
    private var currentUri: Uri? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> { player?.play(); updateNotification() }
            ACTION_PAUSE -> { player?.pause(); updateNotification() }
            ACTION_PLAY_FILE -> {
                val s = intent.getStringExtra("fileUri") ?: return START_STICKY
                currentUri = Uri.parse(s)
                player?.setMediaItem(MediaItem.fromUri(currentUri!!))
                player?.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                player?.prepare()
                player?.play()
                startForeground(NOTIF_ID, buildNotification())
            }
            ACTION_PREV, ACTION_NEXT -> { /* implement if you keep a playlist here */ }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val pauseIntent = PendingIntent.getService(this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_PAUSE), PendingIntent.FLAG_IMMUTABLE)
        val playIntent = PendingIntent.getService(this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_PLAY), PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentUri?.lastPathSegment ?: "Music")
            .setContentText("Playing")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_media_play, "Play", playIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())

        return builder.build()
    }

    private fun updateNotification() {
        val notif = buildNotification()
        val nm = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.notify(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
