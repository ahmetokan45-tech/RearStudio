package de.rearstudio

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/** Samples the rendered composite, including transparency, using an exact inverse display transform. */
object PixelPicker {
    fun show(context: Context, source: StudioCanvas, initial: Int, apply: (Int) -> Unit) {
        if(source.width<=0||source.height<=0)return
        val bitmap=Bitmap.createBitmap(source.width,source.height,Bitmap.Config.ARGB_8888)
        val selection=source.selected;val guides=source.cameraGuides
        try { source.selected=null;source.cameraGuides=false;source.draw(Canvas(bitmap)) }
        finally { source.selected=selection;source.cameraGuides=guides;source.invalidate() }
        var color=initial
        val body=StudioUi.column(context)
        val label=StudioUi.label(context,StudioUi.text(context,"sample_composite"),14f)
        body.addView(label)
        val sample=object:View(context){
            val paint=Paint(Paint.ANTI_ALIAS_FLAG)
            var px=bitmap.width/2;var py=bitmap.height/2
            var scale=1f;var left=0f;var top=0f
            override fun onMeasure(w:Int,h:Int){val width=MeasureSpec.getSize(w);setMeasuredDimension(width,(width*bitmap.height.toFloat()/bitmap.width).toInt()+StudioUi.dp(context,120))}
            override fun onDraw(c:Canvas){
                super.onDraw(c);scale=width.toFloat()/bitmap.width
                c.drawBitmap(bitmap,null,RectF(0f,0f,width.toFloat(),bitmap.height*scale),paint)
                val radius=StudioUi.dp(context,40).toFloat();val cx=width/2f;val cy=bitmap.height*scale+radius+12
                c.save();c.clipPath(Path().apply{addCircle(cx,cy,radius,Path.Direction.CW)})
                val zoom=8f;c.translate(cx-px*zoom,cy-py*zoom);c.scale(zoom,zoom)
                paint.isFilterBitmap=false;c.drawBitmap(bitmap,0f,0f,paint);c.restore()
                paint.color=Color.WHITE;paint.strokeWidth=2f
                c.drawLine(cx-12,cy,cx+12,cy,paint);c.drawLine(cx,cy-12,cx,cy+12,paint)
                c.drawCircle(px*scale,py*scale,6f,paint)
            }
            override fun onTouchEvent(e:MotionEvent):Boolean {
                if(e.actionMasked==MotionEvent.ACTION_DOWN||e.actionMasked==MotionEvent.ACTION_MOVE||e.actionMasked==MotionEvent.ACTION_UP){
                    px=(e.x/scale).toInt().coerceIn(0,bitmap.width-1);py=(e.y/scale).toInt().coerceIn(0,bitmap.height-1)
                    color=bitmap.getPixel(px,py)
                    label.text=String.format(java.util.Locale.ROOT,"#%08X · RGB %d, %d, %d · %d × %d",color,Color.red(color),Color.green(color),Color.blue(color),px,py)
                    invalidate();if(e.actionMasked==MotionEvent.ACTION_UP)performClick();return true
                };return super.onTouchEvent(e)
            }
            override fun performClick():Boolean{super.performClick();return true}
        }
        body.addView(sample,LinearLayout.LayoutParams(-1,-2))
        MaterialAlertDialogBuilder(context).setTitle(StudioUi.text(context,"eyedropper")).setView(body)
            .setNegativeButton(StudioUi.text(context,"cancel"),null).setPositiveButton(StudioUi.text(context,"apply_design")){_,_->apply(color)}
            .create().apply{setOnDismissListener{bitmap.recycle()};show()}
    }
}
