package com.example.dualplayer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    // Players
    private var discoursePlayer: ExoPlayer? = null
    private var musicPlayer: ExoPlayer? = null

    // UI
    private lateinit var selectDiscourseBtn: Button
    private lateinit var playPauseDiscourseBtn: Button
    private lateinit var nextDiscourseBtn: Button
    private lateinit var discourseVolume: SeekBar
    private lateinit var discourseTitle: TextView

    private lateinit var selectMusicBtn: Button
    private lateinit var playPauseMusicBtn: Button
    private lateinit var musicVolume: SeekBar
    private lateinit var musicTitle: TextView

    // State
    private var discourseFolderUri: Uri? = null
    private var musicFolderUri: Uri? = null

    private var discourseFiles: List<DocumentFile> = emptyList()
    private var musicFiles: List<DocumentFile> = emptyList()
    private var currentDiscourseIndex = 0

    // SharedPreferences key prefix
    private val PREFS = "osho_prefs"
    private val LAST_POS_PREFIX = "last_pos_" // + fileUri

    // Activity result launchers
    private val pickDiscourseFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, IntentFlags)
                discourseFolderUri = uri
                loadDiscourseFolder(uri)
            }
        }

    private val pickMusicFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, IntentFlags)
                musicFolderUri = uri
                loadMusicFolder(uri)
            }
        }

    companion object {
        // persist read permission (readable + writable)
        private const val IntentFlags =
            (android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Bind UI
        selectDiscourseBtn = findViewById(R.id.selectDiscourseFolder)
        playPauseDiscourseBtn = findViewById(R.id.playPauseDiscourse)
        nextDiscourseBtn = findViewById(R.id.nextDiscourse)
        discourseVolume = findViewById(R.id.volumeDiscourse)
        discourseTitle = findViewById(R.id.discourseTitle)

        selectMusicBtn = findViewById(R.id.selectMusicFolder)
        playPauseMusicBtn = findViewById(R.id.playPauseMusic)
        musicVolume = findViewById(R.id.volumeMusic)
        musicTitle = findViewById(R.id.musicTitle)

        // Initialize players
        discoursePlayer = ExoPlayer.Builder(this).build()
        musicPlayer = ExoPlayer.Builder(this).build()

        // Default volumes
        discoursePlayer?.volume = 1.0f
        musicPlayer?.volume = 0.5f
        discourseVolume.progress = 100
        musicVolume.progress = 50

        // Button listeners
        selectDiscourseBtn.setOnClickListener { pickDiscourseFolder.launch(null) }
        selectMusicBtn.setOnClickListener { pickMusicFolder.launch(null) }

        playPauseDiscourseBtn.setOnClickListener {
            val p = discoursePlayer ?: return@setOnClickListener
            if (p.isPlaying) {
                p.pause()
                saveLastPositionForCurrentDiscourse()
                playPauseDiscourseBtn.text = "Play Discourse"
            } else {
                p.play()
                playPauseDiscourseBtn.text = "Pause Discourse"
            }
        }

        playPauseMusicBtn.setOnClickListener {
            val p = musicPlayer ?: return@setOnClickListener
            if (p.isPlaying) {
                p.pause()
                playPauseMusicBtn.text = "Play Music"
            } else {
                p.play()
                playPauseMusicBtn.text = "Pause Music"
            }
        }

        nextDiscourseBtn.setOnClickListener {
            if (discourseFiles.isEmpty()) {
                toast("No discourse folder selected")
                return@setOnClickListener
            }
            currentDiscourseIndex = (currentDiscourseIndex + 1) % discourseFiles.size
            playDiscourseAtIndex(currentDiscourseIndex)
        }

        // Volume sliders
        discourseVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                discoursePlayer?.volume = progress / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        musicVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                musicPlayer?.volume = progress / 100f
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })

        // Try to restore previously selected folder URIs from prefs (optional)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val dUri = prefs.getString("discourse_folder_uri", null)
        val mUri = prefs.getString("music_folder_uri", null)
        if (dUri != null) {
            discourseFolderUri = Uri.parse(dUri)
            loadDiscourseFolder(discourseFolderUri!!)
        }
        if (mUri != null) {
            musicFolderUri = Uri.parse(mUri)
            loadMusicFolder(musicFolderUri!!)
        }
    }

    private fun loadDiscourseFolder(uri: Uri) {
        val doc = DocumentFile.fromTreeUri(this, uri)
        if (doc == null || !doc.isDirectory) {
            toast("Selected path is not a folder")
            return
        }
        // filter audio files
        discourseFiles = doc.listFiles()
            .filter { it.isFile && (it.type?.startsWith("audio") == true) }
            .sortedBy { it.name?.lowercase() } // sort alphabetical
        if (discourseFiles.isEmpty()) {
            toast("No audio files found in selected discourse folder")
            discourseTitle.text = "No discourse selected"
            return
        }
        // save chosen folder uri persistently
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("discourse_folder_uri", uri.toString()).apply()

        // start from first or last known index
        currentDiscourseIndex = 0
        playDiscourseAtIndex(currentDiscourseIndex)
    }

    private fun loadMusicFolder(uri: Uri) {
        val doc = DocumentFile.fromTreeUri(this, uri)
        if (doc == null || !doc.isDirectory) {
            toast("Selected path is not a folder")
            return
        }
        musicFiles = doc.listFiles()
            .filter { it.isFile && (it.type?.startsWith("audio") == true) }
            .sortedBy { it.name?.lowercase() }
        if (musicFiles.isEmpty()) {
            toast("No audio files found in selected music folder")
            musicTitle.text = "No music selected"
            return
        }
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("music_folder_uri", uri.toString()).apply()

        // For background music, we'll just load the first file and loop it.
        playMusicAtIndex(0)
    }

    private fun playDiscourseAtIndex(index: Int) {
        if (discourseFiles.isEmpty()) return
        val file = discourseFiles[index]
        discourseTitle.text = file.name ?: "Discourse"
        val uri = file.uri
        val player = discoursePlayer ?: return
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()

        // Seek to saved position for this file if present
        val lastPos = getLastSavedPosition(uri.toString())
        if (lastPos > 0) {
            // clamp to file length later if needed
            player.seekTo(max(0L, lastPos))
        }
        player.play()
        playPauseDiscourseBtn.text = "Pause Discourse"
    }

    private fun playMusicAtIndex(index: Int) {
        if (musicFiles.isEmpty()) return
        val file = musicFiles[index]
        musicTitle.text = file.name ?: "Music"
        val uri = file.uri
        val player = musicPlayer ?: return
        player.stop()
        player.clearMediaItems()
        val item = MediaItem.fromUri(uri)
        player.setMediaItem(item)
        player.repeatMode = ExoPlayer.REPEAT_MODE_ONE
        player.prepare()
        player.play()
        playPauseMusicBtn.text = "Pause Music"
    }

    // Save last position for current discourse file
    private fun saveLastPositionForCurrentDiscourse() {
        val player = discoursePlayer ?: return
        val currentUri = discourseFiles.getOrNull(currentDiscourseIndex)?.uri ?: return
        val pos = player.currentPosition
        getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(LAST_POS_PREFIX + currentUri.toString(), pos)
            .apply()
    }

    private fun getLastSavedPosition(fileUriString: String): Long {
        return getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(LAST_POS_PREFIX + fileUriString, 0L)
    }

    override fun onStop() {
        super.onStop()
        // Save discourse position when app goes background
        saveLastPositionForCurrentDiscourse()
        discoursePlayer?.pause()
        musicPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        discoursePlayer?.release()
        musicPlayer?.release()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
