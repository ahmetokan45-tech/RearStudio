package de.rearstudio

import android.content.Context
import android.graphics.Path
import android.graphics.RectF
import android.view.RoundedCorner
import android.view.Surface
import de.rearstudio.core.BackScreen
import org.json.JSONArray
import org.json.JSONObject

/** Geometry uses natural-display pixels. It never constrains element coordinates. */
data class LensCircle(val x: Float, val y: Float, val radius: Float) {
    fun json() = JSONObject().put("x", x).put("y", y).put("radius", radius)
}

data class DisplayGeometry(
    val width: Float = 976f,
    val height: Float = 596f,
    // TL, TR, BR, BL. Fallback from the inherited device report, not a new measurement.
    val corners: List<Float> = List(4) { 101f },
    val lenses: List<LensCircle> = emptyList(),
    val source: String = "geometry_estimated"
) {
    init {
        require(width.isFinite() && height.isFinite() && width in 1f..16384f && height in 1f..16384f)
        require(corners.size == 4 && corners.all { it.isFinite() && it >= 0f && it <= minOf(width, height) / 2f })
        require(lenses.size <= 2 && lenses.all { it.x.isFinite() && it.y.isFinite() && it.radius.isFinite() && it.radius > 0f && it.radius <= maxOf(width,height) && it.x in 0f..width && it.y in 0f..height })
    }

    /** Inset is in output pixels, used for the stroke centre of an edge animation. */
    fun outline(outputWidth: Float, outputHeight: Float, inset: Float = 0f): Path {
        val path = Path()
        if(outputWidth <= 0 || outputHeight <= 0 || !inset.isFinite()) return path
        val d = inset.coerceIn(0f, minOf(outputWidth,outputHeight)/2f)
        val radii = corners.flatMap { radius -> listOf(
            (radius * outputWidth / width - d).coerceAtLeast(0f),
            (radius * outputHeight / height - d).coerceAtLeast(0f)
        ) }.toFloatArray()
        path.addRoundRect(RectF(d,d,outputWidth-d,outputHeight-d), radii, Path.Direction.CW)
        return path
    }

    fun json() = JSONObject().put("width",width).put("height",height)
        .put("corners",JSONArray(corners)).put("lenses",JSONArray(lenses.map { it.json() })).put("source",source)

    companion object {
        fun read(o: JSONObject): DisplayGeometry {
            val c=o.getJSONArray("corners");require(c.length()==4)
            val l=o.optJSONArray("lenses")?:JSONArray();require(l.length()<=2)
            return DisplayGeometry(o.getDouble("width").toFloat(),o.getDouble("height").toFloat(),
                List(4){c.getDouble(it).toFloat()}, List(l.length()){
                    val lens=l.getJSONObject(it)
                    LensCircle(lens.getDouble("x").toFloat(),lens.getDouble("y").toFloat(),lens.getDouble("radius").toFloat())
                }, "geometry_custom")
        }

        fun detect(context: Context): DisplayGeometry {
            val display=BackScreen.findRearDisplay(context)?:return DisplayGeometry()
            return runCatching {
                val size=BackScreen.naturalSize(display)
                val positions=listOf(RoundedCorner.POSITION_TOP_LEFT,RoundedCorner.POSITION_TOP_RIGHT,
                    RoundedCorner.POSITION_BOTTOM_RIGHT,RoundedCorner.POSITION_BOTTOM_LEFT)
                val current=positions.map { display.getRoundedCorner(it)?.radius?.toFloat() }
                // Convert physical corner ordering back to natural landscape orientation.
                val shift=when(display.rotation){Surface.ROTATION_90->3;Surface.ROTATION_180->2;Surface.ROTATION_270->1;else->0}
                val natural=List(4){current[(it+shift)%4]}
                val fallback=101f/596f*size.height
                DisplayGeometry(size.width.toFloat(),size.height.toFloat(),natural.map { it?:fallback },
                    source=if(natural.all{it!=null})"geometry_device" else "geometry_estimated")
            }.getOrElse { DisplayGeometry() }
        }
    }
}
