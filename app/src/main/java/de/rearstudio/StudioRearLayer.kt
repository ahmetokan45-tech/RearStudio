package de.rearstudio
import android.content.Context
import android.widget.FrameLayout
class StudioRearLayer(context:Context):FrameLayout(context){
 private val canvas=StudioCanvas(context).apply{forcedProfile=null}
 var onFrame:(()->Unit)?=null
 fun ownsGestures()=canvas.scene.navigation.optBoolean("enabled",false)
 fun closeSmartView():Boolean{if(canvas.smartViews.opened==null)return false;canvas.smartViews.close();canvas.refresh();return true}
 fun setRuntimeActive(active:Boolean)=canvas.setRuntimeActive(active)
 init{canvas.onFrame={onFrame?.invoke()};addView(canvas,LayoutParams(-1,-1))}
 override fun onDetachedFromWindow(){canvas.dispose();super.onDetachedFromWindow()}
}
