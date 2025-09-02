package com.example.dualplayer

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class MainActivity : AppCompatActivity() {

    private lateinit var discoursePlayer: ExoPlayer
    private lateinit var musicPlayer: ExoPlayer
    private var isDiscoursePlaying = false
    private var isMusicPlaying = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize players
        discoursePlayer = ExoPlayer.Builder(this).build()
        musicPlayer = ExoPlayer.Builder(this).build()

        // Load local audio from res/raw
        val discourseUri = Uri.parse("android.resource://${packageName}/raw/discourse")
        val musicUri = Uri.parse("android.resource://${packageName}/raw/music")

        discoursePlayer.setMediaItem(MediaItem.fromUri(discourseUri))
        musicPlayer.setMediaItem(MediaItem.fromUri(musicUri))

        discoursePlayer.prepare()
        musicPlayer.prepare()

        val playDiscourseBtn = findViewById<Button>(R.id.playDiscourseBtn)
        val playMusicBtn = findViewById<Button>(R.id.playMusicBtn)
        val discourseVolume = findViewById<SeekBar>(R.id.discourseVolume)
        val musicVolume = findViewById<SeekBar>(R.id.musicVolume)

        discourseVolume.progress = 100
        musicVolume.progress = 50

        playDiscourseBtn.setOnClickListener {
            if (isDiscoursePlaying) discoursePlayer.pause() else discoursePlayer.play()
            isDiscoursePlaying = !isDiscoursePlaying
        }

        playMusicBtn.setOnClickListener {
            if (isMusicPlaying) musicPlayer.pause() else musicPlayer.play()
            isMusicPlaying = !isMusicPlaying
        }

        discourseVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                discoursePlayer.volume = progress / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        musicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                musicPlayer.volume = progress / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        discoursePlayer.release()
        musicPlayer.release()
    }
}
