package de.rearstudio

import org.json.JSONObject
import kotlin.math.abs

enum class SwipeDirection { UP, DOWN, LEFT, RIGHT }

/** Navigation never owns or mutates a scene snapshot. */
class SmartViews {
    var opened: String? = null
        private set
    private var lastInteraction = 0L
    fun open(view: String, now: Long) { require(view in VIEWS); opened = view; lastInteraction = now }
    fun close() { opened = null }
    fun interact(now: Long) { lastInteraction = now }
    fun expire(now: Long, timeoutSeconds: Int): Boolean {
        if (opened == null || timeoutSeconds <= 0 || now-lastInteraction < timeoutSeconds*1000L) return false
        close(); return true
    }
    fun swipe(direction: SwipeDirection, settings: JSONObject, now: Long) {
        if (opened != null) {
            if (direction.name == settings.optString("close", "DOWN")) close() else interact(now)
        } else {
            val view = settings.optString(direction.name, DEFAULTS.getValue(direction))
            if (view in VIEWS) open(view, now)
        }
    }
    companion object {
        val VIEWS = listOf("smart_activity", "smart_notifications", "smart_weather", "smart_music")
        val DEFAULTS = mapOf(SwipeDirection.UP to VIEWS[0], SwipeDirection.DOWN to VIEWS[1], SwipeDirection.LEFT to VIEWS[2], SwipeDirection.RIGHT to VIEWS[3])
        fun direction(dx: Float, dy: Float, threshold: Float): SwipeDirection? = when {
            !dx.isFinite() || !dy.isFinite() || maxOf(abs(dx),abs(dy)) < threshold -> null
            abs(dx)>abs(dy) -> if(dx>0)SwipeDirection.RIGHT else SwipeDirection.LEFT
            else -> if(dy>0)SwipeDirection.DOWN else SwipeDirection.UP
        }
    }
}
