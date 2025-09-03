package com.example.dualplayer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity(), PlayerAdapter.Listener {

    private lateinit var discTitle: TextView
    private lateinit var discPlay: ImageButton
    private lateinit var discPrev: ImageButton
    private lateinit var discNext: ImageButton
    private lateinit var discSeek: SeekBar
    private lateinit var discVolume: SeekBar

    private lateinit var musicTitle: TextView
    private lateinit var musicPlay: ImageButton
    private lateinit var musicPrev: ImageButton
    private lateinit var musicNext: ImageButton
    private lateinit var musicSeek: SeekBar
    private lateinit var musicVolume: SeekBar

    private lateinit var selectDiscBtn: Button
    private lateinit var selectMusicBtn: Button

    private lateinit var playlistRecycler: RecyclerView
    private lateinit var adapter: PlayerAdapter

    // current playlist (document files)
    private var currentList: List<DocumentFile> = emptyList()
    private var currentIsDiscourse = true

    private val prefsName = "osho_prefs"

    private val pickFolderLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri == null) return@registerForActivityResult
            // keep persistable permission
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val picked = DocumentFile.fromTreeUri(this, uri)
            if (picked != null && picked.isDirectory) {
                val audios = picked.listFiles()
                    .filter { it.isFile && (it.type?.startsWith("audio") == true) }
                    .sortedBy { it.name?.lowercase() }
                if (audios.isEmpty()) {
                    Toast.makeText(this, "No audio files found", Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                // store folder uri
                val key = if (currentIsDiscourse) "discourse_folder_uri" else "music_folder_uri"
                getSharedPreferences(prefsName, Context.MODE_PRIVATE).edit()
                    .putString(key, uri.toString()).apply()

                currentList = audios
                adapter.submitList(currentList)
                playlistRecycler.scrollToPosition(0)

                // auto play first item (send to corresponding service)
                val firstUri = currentList[0].uri
                if (currentIsDiscourse) {
                    discTitle.text = currentList[0].name
                    val i = Intent(this, DiscourseService::class.java).apply {
                        action = DiscourseService.ACTION_PLAY_FILE
                        putExtra("fileUri", firstUri.toString())
                    }
                    startService(i)
                    discPlay.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    musicTitle.text = currentList[0].name
                    val i = Intent(this, MusicService::class.java).apply {
                        action = MusicService.ACTION_PLAY_FILE
                        putExtra("fileUri", firstUri.toString())
                    }
                    startService(i)
                    musicPlay.setImageResource(android.R.drawable.ic_media_pause)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        adapter = PlayerAdapter(this)
        playlistRecycler.adapter = adapter
        playlistRecycler.layoutManager = LinearLayoutManager(this)

        selectDiscBtn.setOnClickListener {
            currentIsDiscourse = true
            pickFolderLauncher.launch(null)
        }
        selectMusicBtn.setOnClickListener {
            currentIsDiscourse = false
            pickFolderLauncher.launch(null)
        }

        // Buttons send Intents to services
        discPlay.setOnClickListener {
            val action = if (discPlay.tag == "playing") DiscourseService.ACTION_PAUSE else DiscourseService.ACTION_PLAY
            startService(Intent(this, DiscourseService::class.java).setAction(action))
            toggleButtonState(discPlay)
        }
        discPrev.setOnClickListener {
            startService(Intent(this, DiscourseService::class.java).setAction(DiscourseService.ACTION_PREV))
        }
        discNext.setOnClickListener {
            startService(Intent(this, DiscourseService::class.java).setAction(DiscourseService.ACTION_NEXT))
        }

        musicPlay.setOnClickListener {
            val action = if (musicPlay.tag == "playing") MusicService.ACTION_PAUSE else MusicService.ACTION_PLAY
            startService(Intent(this, MusicService::class.java).setAction(action))
            toggleButtonState(musicPlay)
        }
        musicPrev.setOnClickListener {
            startService(Intent(this, MusicService::class.java).setAction(MusicService.ACTION_PREV))
        }
        musicNext.setOnClickListener {
            startService(Intent(this, MusicService::class.java).setAction(MusicService.ACTION_NEXT))
        }

        // load saved folder URIs if present and populate list for discourse by default
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.getString("discourse_folder_uri", null)?.let {
            tryRestoreFolder(Uri.parse(it), true)
        }
        prefs.getString("music_folder_uri", null)?.let {
            // keep music folder if you want; don't auto-list to avoid confusion
        }
    }

    private fun tryRestoreFolder(uri: Uri, isDiscourse: Boolean) {
        val doc = DocumentFile.fromTreeUri(this, uri)
        if (doc != null && doc.isDirectory) {
            val audios = doc.listFiles().filter { it.isFile && (it.type?.startsWith("audio") == true) }
                .sortedBy { it.name?.lowercase() }
            if (audios.isNotEmpty() && isDiscourse) {
                currentIsDiscourse = true
                currentList = audios
                adapter.submitList(currentList)
                discTitle.text = currentList[0].name
            }
        }
    }

    private fun bindViews() {
        discTitle = findViewById(R.id.discourseTitle)
        discPlay = findViewById(R.id.discPlayPause)
        discPrev = findViewById(R.id.discPrev)
        discNext = findViewById(R.id.discNext)
        discSeek = findViewById(R.id.discSeek)
        discVolume = findViewById(R.id.discVolume)
        selectDiscBtn = findViewById(R.id.selectDiscourseFolder)

        musicTitle = findViewById(R.id.musicTitle)
        musicPlay = findViewById(R.id.musicPlayPause)
        musicPrev = findViewById(R.id.musicPrev)
        musicNext = findViewById(R.id.musicNext)
        musicSeek = findViewById(R.id.musicSeek)
        musicVolume = findViewById(R.id.musicVolume)
        selectMusicBtn = findViewById(R.id.selectMusicFolder)

        playlistRecycler = findViewById(R.id.playlistRecycler)
    }

    private fun toggleButtonState(btn: ImageButton) {
        val playing = btn.tag == "playing"
        if (playing) {
            btn.setImageResource(android.R.drawable.ic_media_play)
            btn.tag = "paused"
        } else {
            btn.setImageResource(android.R.drawable.ic_media_pause)
            btn.tag = "playing"
        }
    }

    // Recycler item clicked -> play selected file (dispatch to appropriate service)
    override fun onAudioClick(documentFile: DocumentFile) {
        val uri = documentFile.uri
        if (currentIsDiscourse) {
            discTitle.text = documentFile.name
            val i = Intent(this, DiscourseService::class.java).apply {
                action = DiscourseService.ACTION_PLAY_FILE
                putExtra("fileUri", uri.toString())
            }
            startService(i)
            discPlay.setImageResource(android.R.drawable.ic_media_pause)
            discPlay.tag = "playing"
        } else {
            musicTitle.text = documentFile.name
            val i = Intent(this, MusicService::class.java).apply {
                action = MusicService.ACTION_PLAY_FILE
                putExtra("fileUri", uri.toString())
            }
            startService(i)
            musicPlay.setImageResource(android.R.drawable.ic_media_pause)
            musicPlay.tag = "playing"
        }
    }
}
