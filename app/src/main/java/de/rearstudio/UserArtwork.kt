package de.rearstudio

import android.content.Context
import android.graphics.*
import java.io.File

/** Original artwork bytes; overlays have their own fixed 976 x 596 reference canvas. */
object UserArtwork {
 val backgrounds=linkedMapOf("99754" to "Blue Silver Flow", "99757" to "Gold Titanium Original", "99750" to "Sunset Watchtower", "99751" to "Synthwave Sunset", "99760" to "Digital City", "99759" to "Mountain Sunrise", "99823" to "Mountain Sunrise II", "99758" to "Bordeaux Geometry", "99815" to "Black Geometry")
 val overlays=linkedMapOf("" to "Aus / Off", "102142" to "Orange Oval", "102141" to "Orange Equalizer", "102283" to "Rainbow Glow I", "102284" to "Rainbow Glow II", "102285" to "Rainbow Rings I", "102286" to "Rainbow Rings II")
 fun validOverlay(id:String)=id.takeIf{overlays.containsKey(it)}?:""
 fun install(context:Context){
  val prefs=context.getSharedPreferences("studio_backgrounds",0)
  for((id,title) in backgrounds){
   val file=File(context.filesDir,"scene-premium-user-$id.jpg")
   if(file.exists())continue
   val temporary=File(context.filesDir,file.name+".tmp")
   context.assets.open("studio/$id.jpg").use{input->temporary.outputStream().use{input.copyTo(it)}}
   check(temporary.renameTo(file))
   prefs.edit().putString("name:${file.name}",title).apply()
  }
 }
 fun bounds(width:Int,height:Int):RectF {
  val scale=minOf(width/976f,height/596f)
  return RectF((width-976*scale)/2,(height-596*scale)/2,(width+976*scale)/2,(height+596*scale)/2)
 }
 class Renderer(private val context:Context){
  private var key=""
  private var bitmap:Bitmap?=null
  private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
  fun draw(canvas:Canvas,id:String,width:Int,height:Int){
   val selected=validOverlay(id)
   if(selected!=key){
    bitmap?.recycle();bitmap=null;key=selected
    if(selected.isNotEmpty())bitmap=context.assets.open("studio/$selected.png").use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inScaled=false})}
   }
   bitmap?.let{canvas.drawBitmap(it,null,bounds(width,height),paint)}
  }
 }
}
