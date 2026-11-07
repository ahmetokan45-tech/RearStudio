package de.rearstudio
import android.content.Context
import android.widget.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
object LightComparisonDialog {
 fun show(context:Context,source:StudioScene,profile:String){
  val row=LinearLayout(context);val canvases=mutableListOf<StudioCanvas>()
  fun t(key:String)=StudioUi.text(context,key)
  repeat(2){index->
   val column=StudioUi.column(context);row.addView(column,LinearLayout.LayoutParams(0,-2,1f))
   column.addView(StudioUi.label(context,if(index==0)"A" else "B",18f))
   val snapshot=source.copy();val settings=snapshot.profile(profile)
   val frame=RearPreview(context).apply{geometry=source.geometry?:DisplayGeometry.detect(context)}
   val canvas=StudioCanvas(context).apply{scene=snapshot;forcedProfile=profile;cameraGuides=true};canvases.add(canvas)
   frame.addView(canvas,FrameLayout.LayoutParams(-1,-1));column.addView(frame)
   StudioUi.button(context,column,t("animation")){
    val effects=listOf("static","pulse","blink","wave","chase")
    MaterialAlertDialogBuilder(context).setItems(effects.map(::t).toTypedArray()){_,i->settings.cameraLight=settings.cameraLight.copy(effect=effects[i],enabled=true);canvas.restartAnimation();canvas.invalidate()}.show()
   }
   StudioUi.button(context,column,t("palette")){
    val values=listOf("classic","dual","gradient","rainbow")
    MaterialAlertDialogBuilder(context).setItems(values.map(::t).toTypedArray()){_,i->settings.cameraLight=settings.cameraLight.copy(palette=values[i]);canvas.invalidate()}.show()
   }
   StudioUi.button(context,column,t("play_pause")){canvas.playback(!canvas.previewPlaying)}
   StudioUi.button(context,column,t("restart_preview")){canvas.restartAnimation();canvas.playback(true)}
  }
  MaterialAlertDialogBuilder(context).setTitle(t("compare_lights")).setView(row).setPositiveButton(t("done"),null).create().apply{setOnDismissListener{canvases.forEach{it.dispose()}};show()}
 }
}
