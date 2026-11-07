package de.rearstudio

import org.json.JSONArray
import org.json.JSONObject

/** Sorted, bounded gradient stops; malformed imports fall back to the legacy two colours. */
data class GradientStop(val position: Float, val color: Int)
data class GradientSpec(val stops: List<GradientStop>, val angle: Float, val periodMs: Long, val animated: Boolean) {
    fun angleAt(elapsed: Long, reducedMotion: Boolean): Float = angle +
        if (animated && !reducedMotion) (elapsed.coerceAtLeast(0) % periodMs) * 360f / periodMs else 0f
    companion object {
        fun read(widget: StudioWidget): GradientSpec {
            val source = widget.options.optJSONArray("stops")
            val stops = (0 until minOf(source?.length() ?: 0, 8)).mapNotNull { i ->
                val item = source?.optJSONObject(i) ?: return@mapNotNull null
                val position = item.optDouble("position", Double.NaN)
                if (!position.isFinite()) null else GradientStop(position.toFloat().coerceIn(0f, 1f), item.optInt("color", widget.color))
            }.sortedBy { it.position }.distinctBy { it.position }
            val safe = if (stops.size >= 2) stops else listOf(GradientStop(0f, widget.color), GradientStop(1f, widget.options.optInt("color2", widget.color)))
            val angle = widget.options.optDouble("angle", 45.0).let { if (it.isFinite()) it.toFloat() % 360 else 45f }
            return GradientSpec(safe, angle, widget.options.optLong("period", 5000).coerceIn(500, 60000), widget.options.optBoolean("animateGradient"))
        }
        fun write(widget: StudioWidget, stops: List<GradientStop>) {
            require(stops.size in 2..8)
            require(stops.all { it.position.isFinite() && it.position in 0f..1f })
            require(stops.map { it.position }.distinct().size == stops.size)
            widget.options.put("stops", JSONArray(stops.sortedBy { it.position }.map { JSONObject().put("position", it.position).put("color", it.color) }))
        }
    }
}
