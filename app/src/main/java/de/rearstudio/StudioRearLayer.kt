package de.rearstudio
import android.content.Context
import android.widget.FrameLayout
class StudioRearLayer(context:Context):FrameLayout(context){
 private val canvas=StudioCanvas(context).apply{forcedProfile=null}
 private var switcher:QuickDesignSwitcher?=null
 var onFrame:(()->Unit)?=null
 fun ownsGestures()=canvas.scene.navigation.optBoolean("enabled",false)||canvas.scene.navigation.optBoolean("quickSwitcher",false)
 fun closeSmartView():Boolean{
   switcher?.let{removeView(it);switcher=null;canvas.setRuntimeActive(true);canvas.refresh();return true}
   if(canvas.smartViews.opened==null)return false;canvas.smartViews.close();canvas.refresh();return true
 }
 fun setRuntimeActive(active:Boolean){if(!active)switcher?.let{removeView(it);switcher=null};canvas.setRuntimeActive(active&&switcher==null)}
 init{
   canvas.onFrame={onFrame?.invoke()};addView(canvas,LayoutParams(-1,-1))
   canvas.onQuickSwitch={
     if(switcher==null){canvas.setRuntimeActive(false);switcher=QuickDesignSwitcher(context,StudioScene.load(context)){closeSmartView()};addView(switcher,LayoutParams(-1,-1))}
   }
 }
 override fun onDetachedFromWindow(){canvas.dispose();super.onDetachedFromWindow()}
}
