package com.example.dualplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
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

        // Create notification channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Music Playback",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }

        // Setup player
        player = ExoPlayer.Builder(this).build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                player?.play()
                updateNotification()
            }
            ACTION_PAUSE -> {
                player?.pause()
                updateNotification()
            }
            ACTION_PLAY_FILE -> {
                val s = intent.getStringExtra("fileUri") ?: return START_STICKY
                currentUri = Uri.parse(s)

                player?.setMediaItem(MediaItem.fromUri(currentUri!!))
                player?.repeatMode = ExoPlayer.REPEAT_MODE_ONE
                player?.prepare()
                player?.play()

                startForeground(NOTIF_ID, buildNotification())
            }
            ACTION_PREV, ACTION_NEXT -> {
                // You can extend this with a playlist manager
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, MusicService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE
        )

        val playIntent = PendingIntent.getService(
            this, 2,
            Intent(this, MusicService::class.java).setAction(ACTION_PLAY),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(currentUri?.lastPathSegment ?: "Music")
            .setContentText(
                if (player?.isPlaying == true) "Playing" else "Paused"
            )
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_previous, "Prev", null) // TODO hook prev
            .addAction(
                if (player?.isPlaying == true) android.R.drawable.ic_media_pause
                else android.R.drawable.ic_media_play,
                if (player?.isPlaying == true) "Pause" else "Play",
                if (player?.isPlaying == true) pauseIntent else playIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", null) // TODO hook next
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .setOngoing(player?.isPlaying == true)
            .build()
    }

    private fun updateNotification() {
        val notif = buildNotification()
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, notif)
    }

    override fun onDestroy() {
        player?.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
