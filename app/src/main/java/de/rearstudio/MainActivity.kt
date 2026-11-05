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
import android.view.View
import android.view.WindowInsets
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
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
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var sports: SportsPanel
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
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun label(value: String, size: Float = 16f) = TextView(this).apply {
        text = value; textSize = size; setTextColor(Color.WHITE)
    }
    private fun surface(color: Int, radius: Int = 20) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
    }
    private fun button(value: String, parent: LinearLayout, action: () -> Unit) = Button(this).also {
        it.text = value; it.isAllCaps = false; it.textSize = 14f
        it.minWidth = 0; it.minimumWidth = 0; it.minHeight = dp(48)
        it.setTextColor(Color.WHITE); it.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(49, 58, 75))
        parent.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        it.setOnClickListener { action() }
    }
    private fun card(parent: LinearLayout) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16))
        background = surface(Color.rgb(26, 32, 43))
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(20), dp(20), dp(24))
        }
        val scroll = ScrollView(this).apply {
            isFillViewport = true; setBackgroundColor(Color.rgb(15, 20, 29)); addView(root)
            setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
            }
        }
        setContentView(scroll); scroll.requestApplyInsets()
        root.addView(label("AHM3T45", 30f).apply { letterSpacing = .08f })
        root.addView(label("S T U D I O   /   DEIN PERSÖNLICHER HUB", 11f).apply { setTextColor(0xffefbd60.toInt()) })
        val navigation = LinearLayout(this)
        root.addView(navigation, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
        val musicPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val sportPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val settingsPage = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        listOf(musicPage, sportPage, settingsPage).forEach { root.addView(it, LinearLayout.LayoutParams(-1, -2)) }
        sports = SportsPanel(this, sportPage)
        val pages = listOf(musicPage, sportPage, settingsPage)
        val tabs = mutableListOf<Button>()
        fun select(index: Int) {
            pages.forEachIndexed { i, page -> page.visibility = if (i == index) View.VISIBLE else View.GONE }
            tabs.forEachIndexed { i, tab ->
                tab.isSelected = i == index
                tab.setTextColor(if (i == index) 0xff141924.toInt() else Color.WHITE)
                tab.backgroundTintList = android.content.res.ColorStateList.valueOf(if (i == index) 0xffefbd60.toInt() else 0xff202938.toInt())
            }
            preferences.edit().putInt("page", index).apply()
        }
        listOf("Musik", "Sport", "Einstellungen").forEachIndexed { index, name ->
            tabs.add(button(name, navigation) { select(index) }.apply {
                textSize = 12f; setPadding(dp(3), 0, dp(3), 0)
                layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply { if (index > 0) leftMargin = dp(6) }
            })
        }
        select(preferences.getInt("page", 0).coerceIn(0, 2))
        val previewCard = card(musicPage)
        previewCard.addView(label("Album Art", 18f))
        previewCard.addView(label("Vorschau auf dem Hauptdisplay", 12f).apply { setPadding(0, dp(4), 0, dp(12)) })
        val preview = FrameLayout(this).apply {
            background = surface(Color.rgb(62, 48, 74), 16); clipToOutline = true
        }
        previewCard.addView(preview, LinearLayout.LayoutParams(-1, dp(264)))
        cover = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP }
        preview.addView(cover, FrameLayout.LayoutParams(-1, -1))
        preview.addView(View(this).apply { setBackgroundColor(0x99000000.toInt()) }, FrameLayout.LayoutParams(-1, -1))
        val lenses = object : View(this) {
            private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xff8d839c.toInt(); style = Paint.Style.STROKE; strokeWidth = dp(2).toFloat()
            }
            override fun onDraw(canvas: Canvas) {
                val radius = minOf(width * .32f, height * .15f)
                canvas.drawCircle(width / 2f, height * .30f, radius, paint)
                canvas.drawCircle(width / 2f, height * .70f, radius, paint)
            }
        }.apply { contentDescription = "Zwei vorläufige Kamerazonen links" }
        preview.addView(lenses, FrameLayout.LayoutParams(0, -1))
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(12), dp(12), dp(12))
        }
        preview.addView(content, FrameLayout.LayoutParams(0, -1))
        title = label("Keine aktive Musik", 18f).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }
        artist = label("Musikzugriff erlauben, dann einen Player starten.", 13f).apply {
            maxLines = 3; ellipsize = TextUtils.TruncateAt.END; setPadding(0, dp(8), 0, dp(12))
            setTextColor(0xffd3cadf.toInt())
        }
        content.addView(title); content.addView(artist)
        val controls = LinearLayout(this); content.addView(controls)
        previous = button("⏮", controls) { control { skipToPrevious() } }
        play = button("▶", controls) { control { if (controller?.playbackState?.state == PlaybackState.STATE_PLAYING) pause() else play() } }
        next = button("⏭", controls) { control { skipToNext() } }
        listOf(previous, play, next).forEachIndexed { index, button ->
            button.layoutParams = LinearLayout.LayoutParams(0, dp(48), 1f).apply { if (index > 0) leftMargin = dp(4) }
            button.setPadding(0, 0, 0, 0)
        }
        previous.contentDescription = "Vorheriger Titel"; play.contentDescription = "Wiedergabe"; next.contentDescription = "Nächster Titel"
        val design = card(settingsPage)
        design.addView(label("Kamerabereich", 18f))
        val safeLabel = label("", 13f); design.addView(safeLabel)
        val slider = SeekBar(this).apply { max = 35; progress = preferences.getInt("safe", 35).coerceIn(20, 55) - 20 }
        fun safe(value: Int) {
            safeLabel.text = "Links geschützt: $value % · noch nicht kalibriert"
            if (preview.width == 0) return
            val margin = preview.width * value / 100
            val params = content.layoutParams as FrameLayout.LayoutParams
            val available = preview.width - margin
            if (params.leftMargin != margin || params.width != available) {
                params.leftMargin = margin; params.width = available; content.layoutParams = params
            }
            if (lenses.layoutParams.width != margin) {
                lenses.layoutParams = FrameLayout.LayoutParams(margin, -1)
            }
        }
        preview.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> safe(slider.progress + 20) }
        slider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, value: Int, user: Boolean) {
                safe(value + 20); preferences.edit().putInt("safe", value + 20).apply()
            }
            override fun onStartTrackingTouch(bar: SeekBar?) {}
            override fun onStopTrackingTouch(bar: SeekBar?) {}
        }); design.addView(slider); safe(slider.progress + 20)
        val access = card(settingsPage)
        access.addView(label("Verbindungen", 18f))
        button("1 · Musikzugriff erlauben", access) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        status = label("", 14f).apply { setPadding(0, dp(12), 0, dp(8)); setLineSpacing(dp(3).toFloat(), 1f) }
        access.addView(status)
        button("2 · Shizuku öffnen / freigeben", access) {
            try {
                when (ShizukuState.read(this)) {
                    ShizukuState.NOT_ALLOWED -> Shizuku.requestPermission(1)
                    else -> ShizukuState.openIntent(this)?.let { startActivity(it) }
                        ?: Toast.makeText(this, "Bitte Shizuku zuerst installieren und starten.", Toast.LENGTH_LONG).show()
                }
            } catch (e: RuntimeException) { status.text = "Shizuku: ${e.message}" }
        }
        root.addView(label("Rückdisplay-Ausgabe noch nicht implementiert. HyperOS 4 China-ROM ist ungetestet.", 12f).apply {
            setPadding(0, dp(20), 0, 0); setTextColor(0xffacb4c9.toInt())
        })
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
        previous.isEnabled = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
        next.isEnabled = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
        play.contentDescription = if (playing) "Pause" else "Wiedergabe"
        play.text = if (playing) "Ⅱ" else "▶"
        play.isEnabled = actions and (if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) != 0L || actions and PlaybackState.ACTION_PLAY_PAUSE != 0L
    }
    private fun refreshStatus() = runOnUiThread {
        if (::status.isInitialized) {
            val displays = getSystemService(DisplayManager::class.java).displays.map { d -> DisplayCandidate(d.displayId, d.mode.physicalWidth, d.mode.physicalHeight, d.flags and android.view.Display.FLAG_PRIVATE != 0) }
            val id = RearDisplayPick.choose(displays, null)
            val state = when (ShizukuState.read(this)) {
                ShizukuState.READY -> "bereit und freigegeben"
                ShizukuState.NOT_ALLOWED -> "Freigabe erforderlich"
                ShizukuState.BLOCKED -> "in Shizuku manuell freigeben"
                ShizukuState.NOT_RUNNING -> "nicht gestartet – bitte in Shizuku starten"
                ShizukuState.NOT_INSTALLED -> "nicht installiert"
            }
            status.text = "Shizuku: $state\nRückdisplay-Kandidat: ${id ?: "nicht erkannt"}"
        }
    }
    override fun onPause() { manager.removeOnActiveSessionsChangedListener(sessionsListener); controller?.unregisterCallback(mediaCallback); super.onPause() }
    override fun onDestroy() {
        sports.close()
        Shizuku.removeRequestPermissionResultListener(permissionListener); Shizuku.removeBinderReceivedListener(binderListener); Shizuku.removeBinderDeadListener(deadListener)
        super.onDestroy()
    }
}
