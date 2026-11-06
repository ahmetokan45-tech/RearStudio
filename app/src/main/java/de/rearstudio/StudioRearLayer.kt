package de.rearstudio
import android.content.Context
import android.widget.FrameLayout
class StudioRearLayer(context:Context):FrameLayout(context){
 private val canvas=StudioCanvas(context).apply{forcedProfile=null}
 var onFrame:(()->Unit)?=null
 init{addView(StudioVideoLayer(context){StudioScene.load(context,true).profile("normal")},LayoutParams(-1,-1));canvas.onFrame={onFrame?.invoke()};addView(canvas,LayoutParams(-1,-1))}
 override fun onDetachedFromWindow(){canvas.dispose();super.onDetachedFromWindow()}
}
