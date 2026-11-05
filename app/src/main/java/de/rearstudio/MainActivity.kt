package de.rearstudio

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null
    private lateinit var title: TextView
    private lateinit var artist: TextView
    private lateinit var status: TextView
    private lateinit var cover: ImageView
    private lateinit var content: LinearLayout
    private lateinit var play: Button
    private val preferences by lazy { getSharedPreferences("design", MODE_PRIVATE) }
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, _ -> refreshStatus() }
    private val binderListener = Shizuku.OnBinderReceivedListener { refreshStatus() }
    private val deadListener = Shizuku.OnBinderDeadListener { refreshStatus() }
    private val mediaCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = renderMusic()
        override fun onPlaybackStateChanged(state: PlaybackState?) = renderMusic()
        override fun onSessionDestroyed() = connectMusic()
    }
    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { connectMusic() }
    private val manager by lazy { getSystemService(MediaSessionManager::class.java) }
    private fun label(value: String, size: Float = 16f) = TextView(this).apply { text = value; textSize = size; setTextColor(Color.WHITE) }
    private fun button(value: String, parent: LinearLayout, action: () -> Unit) = Button(this).also {
        it.text = value; parent.addView(it); it.setOnClickListener { action() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(20, 30, 20, 20); setBackgroundColor(Color.rgb(20, 22, 28)) }
        setContentView(ScrollView(this).apply { addView(root) })
        root.addView(label("Rear Studio · Album Art", 26f))
        root.addView(label("Hauptdisplay-Vorschau. Rückdisplay / HyperOS 4 China-ROM: ungetestet."))
        val preview = FrameLayout(this)
        root.addView(preview, LinearLayout.LayoutParams(-1, 330))
        cover = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(Color.rgb(62, 48, 74)) }
        preview.addView(cover, FrameLayout.LayoutParams(-1, -1))
        preview.addView(android.view.View(this).apply { setBackgroundColor(0x88000000.toInt()) }, FrameLayout.LayoutParams(-1, -1))
        val lenses = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER }
        lenses.addView(label("◯", 48f)); lenses.addView(label("◯", 48f))
        preview.addView(lenses, FrameLayout.LayoutParams(100, -1))
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(8, 8, 8, 8) }
        preview.addView(content, FrameLayout.LayoutParams(-1, -1))
        title = label("Keine aktive Musik", 22f); artist = label("Musikzugriff freigeben und Player starten")
        content.addView(title); content.addView(artist)
        val controls = LinearLayout(this); content.addView(controls)
        button("⏮", controls) { control { skipToPrevious() } }
        play = button("▶", controls) { control { if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) pause() else play() } }
        button("⏭", controls) { control { skipToNext() } }
        root.addView(label("Kamera-Safe-Area links (vorläufig, auf Gerät kalibrieren)"))
        val slider = SeekBar(this).apply { max = 35; progress = preferences.getInt("safe", 35) - 20 }
        fun safe(value: Int) { preview.post { (content.layoutParams as FrameLayout.LayoutParams).also { val margin = preview.width * value / 100; if (it.leftMargin != margin) { it.leftMargin = margin; content.layoutParams = it } } } }
        safe(slider.progress + 20)
        preview.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> safe(slider.progress + 20) }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, user: Boolean) { safe(value + 20); preferences.edit().putInt("safe", value + 20).apply() }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        }); root.addView(slider)
        button("Musikzugriff in Einstellungen", root) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        status = label(""); root.addView(status)
        button("Shizuku freigeben", root) {
            try {
                when (ShizukuState.read(this)) {
                    ShizukuState.NOT_ALLOWED -> Shizuku.requestPermission(1)
                    else -> ShizukuState.openIntent(this)?.let { startActivity(it) }
                }
            } catch (e: RuntimeException) { status.text = "Shizuku: ${e.message}" }
        }
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Shizuku.addBinderDeadListener(deadListener)
    }
    override fun onResume() {
        super.onResume(); refreshStatus()
        try { manager.removeOnActiveSessionsChangedListener(sessionsListener); manager.addOnActiveSessionsChangedListener(sessionsListener, ComponentName(this, MusicAccessService::class.java), handler) } catch (_: SecurityException) {}
        connectMusic()
    }
    private fun connectMusic() {
        controller?.unregisterCallback(mediaCallback)
        controller = try {
            val sessions = manager.getActiveSessions(ComponentName(this, MusicAccessService::class.java))
            sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
        } catch (_: SecurityException) { null }
        controller?.registerCallback(mediaCallback, handler); renderMusic()
    }
    private fun control(action: android.media.session.MediaController.TransportControls.() -> Unit) {
        try { controller?.transportControls?.action() } catch (_: RuntimeException) { connectMusic() }
    }
    private fun renderMusic() {
        val metadata = controller?.metadata
        title.text = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Keine aktive Musik"
        artist.text = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Player starten oder Musikzugriff prüfen"
        cover.setImageBitmap(metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART))
        val actions = controller?.playbackState?.actions ?: 0L
        val playing = controller?.playbackState?.state == PlaybackState.STATE_PLAYING
        play.text = if (playing) "Ⅱ" else "▶"
        play.isEnabled = actions and (if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) != 0L || actions and PlaybackState.ACTION_PLAY_PAUSE != 0L
    }
    private fun refreshStatus() = runOnUiThread {
        if (::status.isInitialized) {
            val displays = getSystemService(DisplayManager::class.java).displays.map { d -> DisplayCandidate(d.displayId, d.mode.physicalWidth, d.mode.physicalHeight, d.flags and android.view.Display.FLAG_PRIVATE != 0) }
            val id = RearDisplayPick.choose(displays, null)
            status.text = "Shizuku: ${ShizukuState.read(this)}\nRückdisplay-Kandidat: ${id ?: "nicht erkannt"}\nNur Erkennung; keine Rückdisplay-Ausgabe aktiviert."
        }
    }
    override fun onPause() { manager.removeOnActiveSessionsChangedListener(sessionsListener); controller?.unregisterCallback(mediaCallback); super.onPause() }
    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener); Shizuku.removeBinderReceivedListener(binderListener); Shizuku.removeBinderDeadListener(deadListener)
        super.onDestroy()
    }
}
