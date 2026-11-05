package de.rearstudio

import android.app.Activity
import android.content.ComponentName
import android.graphics.Color
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Experimental, token-gated secondary-display host. No lock-screen bypass or keeper service. */
class RearHostActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val session by lazy { getSharedPreferences("rear_session", 0) }
    private lateinit var body: TextView
    private lateinit var cover: ImageView
    private lateinit var controls: LinearLayout
    private var media: MediaController? = null
    private val tick = object : Runnable {
        override fun run() {
            if (!valid()) { finishAndRemoveTask(); return }
            render(); handler.postDelayed(this, 1000)
        }
    }
    private fun valid() = RearSessionGuard.allows(session.getString("token", null), intent.getStringExtra("token"), session.getInt("display", -1), display?.displayId ?: 0, session.getLong("issued", 0), SystemClock.elapsedRealtime())
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!valid()) { finish(); return }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val frame = FrameLayout(this).apply { setBackgroundColor(0xff10151e.toInt()) }
        cover = ImageView(this).apply { scaleType = ImageView.ScaleType.CENTER_CROP; imageAlpha = 95 }; frame.addView(cover, FrameLayout.LayoutParams(-1, -1))
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
        val scroll = ScrollView(this).apply { addView(column) }
        frame.addView(scroll, FrameLayout.LayoutParams(-1, -1))
        frame.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            val left = frame.width * getSharedPreferences("design", 0).getInt("safe", 35).coerceIn(20, 55) / 100
            val p = scroll.layoutParams as FrameLayout.LayoutParams
            if (p.leftMargin != left || p.width != frame.width - left) { p.leftMargin = left; p.width = frame.width - left; scroll.layoutParams = p }
        }
        column.addView(TextView(this).apply { text = "AHM3T45 STUDIO · GERÄTETEST"; textSize = 10f; setTextColor(0xffefbd60.toInt()) })
        body = TextView(this).apply { textSize = 16f; setTextColor(Color.WHITE) }; column.addView(body)
        controls = LinearLayout(this); column.addView(controls)
        listOf("⏮", "▶ / Ⅱ", "⏭").forEachIndexed { index, value ->
            controls.addView(Button(this).apply {
                text = value; textSize = 12f; minWidth = 0; minimumWidth = 0; setPadding(0, 0, 0, 0)
                contentDescription = listOf("Vorheriger Titel", "Wiedergabe oder Pause", "Nächster Titel")[index]
                setOnClickListener {
                    try { media?.transportControls?.let { c -> when (index) {
                        0 -> c.skipToPrevious()
                        1 -> if (media?.playbackState?.state == PlaybackState.STATE_PLAYING) c.pause() else c.play()
                        else -> c.skipToNext()
                    } } } catch (_: RuntimeException) {}
                }
            }, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        setContentView(frame)
    }
    private fun render() {
        val mode = session.getString("mode", "clock")
        controls.visibility = if (mode == "music") android.view.View.VISIBLE else android.view.View.GONE
        if (mode == "music") {
            media = try {
                val sessions = getSystemService(MediaSessionManager::class.java).getActiveSessions(ComponentName(this, MusicAccessService::class.java))
                sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
            } catch (_: SecurityException) { null }
            val metadata = media?.metadata
            body.text = (metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Keine Musik – Zugriff prüfen und Player starten") + "\n" + (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "")
            cover.setImageBitmap(metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART))
            val actions = media?.playbackState?.actions ?: 0L
            val playing = media?.playbackState?.state == PlaybackState.STATE_PLAYING
            controls.getChildAt(0).isEnabled = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
            controls.getChildAt(1).isEnabled = actions and (if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) != 0L || actions and PlaybackState.ACTION_PLAY_PAUSE != 0L
            controls.getChildAt(2).isEnabled = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
        } else if (mode == "football" || mode == "f1-results") {
            body.text = getSharedPreferences("sport", 0).getString("latest_$mode", "Bitte das Ergebnis zuerst im Sport-Bereich laden.")
        } else body.text = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n\nWenn du diese Uhr auf dem Rückdisplay siehst, ist dieser Startversuch sichtbar angekommen."
    }
    override fun onResume() { super.onResume(); if (::body.isInitialized) handler.post(tick) }
    override fun onPause() { handler.removeCallbacks(tick); super.onPause() }
    override fun onDestroy() { handler.removeCallbacks(tick); super.onDestroy() }
}
