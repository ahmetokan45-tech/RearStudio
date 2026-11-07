package de.rearstudio

import android.graphics.*

/** Only draws outside the explicitly calibrated lens circles. No illustrative camera coordinates. */
object CameraLightRenderer {
    fun draw(canvas: Canvas, geometry: DisplayGeometry, width: Int, height: Int, settings: LightAnimation, elapsed: Long, reducedMotion: Boolean) {
        if (geometry.lenses.size != 2 || !settings.enabled) return
        val frame=settings.frame(elapsed,reducedMotion)
        if(frame.alpha<=0f)return
        val sx=width/geometry.width;val sy=height/geometry.height
        val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{style=Paint.Style.STROKE;strokeWidth=height*.009f;alpha=(frame.alpha*255).toInt()}
        val save=canvas.save()
        for(lens in geometry.lenses){
            val exclusion=Path().apply{addOval(RectF((lens.x-lens.radius)*sx,(lens.y-lens.radius)*sy,(lens.x+lens.radius)*sx,(lens.y+lens.radius)*sy),Path.Direction.CW)}
            canvas.clipOutPath(exclusion)
        }
        for(lens in geometry.lenses){
            val cx=lens.x*sx;val cy=lens.y*sy;val extra=p.strokeWidth
            val ring=RectF((lens.x-lens.radius)*sx-extra,(lens.y-lens.radius)*sy-extra,(lens.x+lens.radius)*sx+extra,(lens.y+lens.radius)*sy+extra)
            p.shader=when(settings.palette){
                "rainbow"->SweepGradient(cx,cy,intArrayOf(Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED),null)
                "dual","gradient"->SweepGradient(cx,cy,intArrayOf(settings.color,settings.color2,settings.color),null)
                else->null
            }
            p.color=settings.color;p.alpha=(frame.alpha*255).toInt()
            if(!reducedMotion&&settings.effect in listOf("wave","chase"))canvas.drawArc(ring,frame.phase*360f,if(settings.effect=="wave")150f else 70f,false,p)
            else canvas.drawOval(ring,p)
        }
        canvas.restoreToCount(save)
    }
}
