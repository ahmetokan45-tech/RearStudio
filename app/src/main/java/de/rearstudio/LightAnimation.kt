package de.rearstudio

import org.json.JSONObject
import kotlin.math.*

/** Deterministic timing shared by previews and renderer, independent of wall clock changes. */
data class LightAnimation(
    val effect: String = "static",
    val palette: String = "classic",
    val color: Int = 0xfffd9b02.toInt(),
    val color2: Int = 0xff91092d.toInt(),
    val brightness: Int = 60,
    val periodMs: Long = 2400,
    val delayMs: Long = 0,
    val fadeMs: Long = 400,
    val repeats: Int = 0,
    val clockwise: Boolean = true,
    val enabled: Boolean = false
) {
    data class Frame(val alpha: Float, val phase: Float)
    fun frame(elapsedMs: Long, reducedMotion: Boolean = false): Frame {
        val elapsed = (elapsedMs - delayMs.coerceAtLeast(0))
        if (!enabled || elapsed < 0) return Frame(0f, 0f)
        val period = periodMs.coerceIn(250, 60_000)
        val total = if (repeats > 0) period * repeats.coerceAtMost(1000) else Long.MAX_VALUE
        if (elapsed >= total) return Frame(0f, 0f)
        val phase = (elapsed % period).toFloat() / period
        val wave = when {
            reducedMotion -> 1f
            effect == "pulse" -> (.55 + .45 * sin(phase * 2 * PI)).toFloat()
            effect == "blink" -> if (phase < .5f) 1f else 0f
            else -> 1f
        }
        val fade = fadeMs.coerceAtLeast(0)
        val inAlpha = if (fade == 0L) 1f else (elapsed.toFloat()/fade).coerceIn(0f,1f)
        val outAlpha = if (fade == 0L || total == Long.MAX_VALUE) 1f else ((total-elapsed).toFloat()/fade).coerceIn(0f,1f)
        return Frame(brightness.coerceIn(0,100)/100f*wave*inAlpha*outAlpha, if(reducedMotion)0f else if(clockwise)phase else 1f-phase)
    }
    fun json()=JSONObject().put("effect",effect).put("palette",palette).put("color",color).put("color2",color2)
        .put("brightness",brightness).put("periodMs",periodMs).put("delayMs",delayMs).put("fadeMs",fadeMs)
        .put("repeats",repeats).put("clockwise",clockwise).put("enabled",enabled)
    companion object {
        fun read(o:JSONObject)=LightAnimation(o.optString("effect","static"),o.optString("palette","classic"),
            o.optInt("color",0xfffd9b02.toInt()),o.optInt("color2",0xff91092d.toInt()),o.optInt("brightness",60).coerceIn(0,100),
            o.optLong("periodMs",2400).coerceIn(250,60_000),o.optLong("delayMs",0).coerceIn(0,60_000),o.optLong("fadeMs",400).coerceIn(0,10_000),
            o.optInt("repeats",0).coerceIn(0,1000),o.optBoolean("clockwise",true),o.optBoolean("enabled",false))
    }
}
