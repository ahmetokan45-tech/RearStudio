package de.rearstudio

import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.View
import android.widget.*

/** Studio content uses the upstream host and keeper; charging and notification layers remain above it. */
class StudioRearLayer(context: Context) : FrameLayout(context) {
    private val handler = Handler(Looper.getMainLooper())
    private val prefs = context.getSharedPreferences("design", 0)
    private val cover = ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP; imageAlpha = 120 }
    private val column = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(8)) }
    private val scroll = ScrollView(context).apply { addView(column) }
    private val body = TextView(context).apply { textSize = 16f; setTextColor(Color.WHITE) }
    private val controls = LinearLayout(context)
    private var media: MediaController? = null
    private var lastArt: android.graphics.Bitmap? = null
    private val ticker = object : Runnable {
        override fun run() {
            val mode = prefs.getInt("studio_mode", 0)
            visibility = if (mode == 0) View.GONE else View.VISIBLE
            if (mode != 0 && display?.state == Display.STATE_ON) render(mode)
            handler.postDelayed(this, 1000)
        }
    }
    init {
        setBackgroundColor(0xff10151e.toInt())
        addView(cover, LayoutParams(-1, -1)); addView(scroll, LayoutParams(-1, -1))
        column.addView(TextView(context).apply { text = "AHM3T45 STUDIO"; textSize = 10f; setTextColor(0xffefbd60.toInt()) })
        column.addView(body); column.addView(controls)
        listOf("⏮", "▶ / Ⅱ", "⏭").forEachIndexed { index, value ->
            controls.addView(Button(context).apply {
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
        addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> safeArea() }
        visibility = View.GONE
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun safeArea() {
        val left = width * prefs.getInt("safe", 35).coerceIn(20, 55) / 100
        val p = scroll.layoutParams as LayoutParams
        if (p.leftMargin != left || p.width != width - left) { p.leftMargin = left; p.width = width - left; scroll.layoutParams = p }
    }
    private fun render(mode: Int) {
        safeArea()
        controls.visibility = if (mode == 1) View.VISIBLE else View.GONE
        if (mode == 1) {
            media = try {
                val sessions = context.getSystemService(MediaSessionManager::class.java).getActiveSessions(ComponentName(context, MusicAccessService::class.java))
                sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sessions.firstOrNull()
            } catch (_: SecurityException) { null }
            val metadata = media?.metadata
            body.text = (metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "Musikzugriff erlauben und Player starten") + "\n" + (metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "")
            val art = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            if (art !== lastArt) { cover.setImageBitmap(art); lastArt = art }
            val actions = media?.playbackState?.actions ?: 0L
            val playing = media?.playbackState?.state == PlaybackState.STATE_PLAYING
            controls.getChildAt(0).isEnabled = actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L
            controls.getChildAt(1).isEnabled = actions and (if (playing) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY) != 0L || actions and PlaybackState.ACTION_PLAY_PAUSE != 0L
            controls.getChildAt(2).isEnabled = actions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L
        } else {
            if (lastArt != null) { cover.setImageBitmap(null); lastArt = null }
            val key = if (mode == 2) "latest_football" else "latest_f1-results"
            body.text = context.getSharedPreferences("sport", 0).getString(key, "Bitte das Ergebnis zuerst im Sport-Bereich laden.")
        }
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); handler.post(ticker) }
    override fun onDetachedFromWindow() { handler.removeCallbacks(ticker); media = null; cover.setImageBitmap(null); lastArt = null; super.onDetachedFromWindow() }
}
