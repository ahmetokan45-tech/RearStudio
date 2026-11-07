package de.rearstudio
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File
object PremiumBackgrounds {
 val names=linkedMapOf(
  "dark_titanium" to "Dark Titanium",
  "liquid_glass" to "Liquid Glass",
  "nebula_blue" to "Nebula Blue",
  "sunset_glow" to "Sunset Glow",
  "aurora" to "Aurora",
  "galaxy" to "Galaxy",
  "red_carbon" to "Red Carbon",
  "gold_titanium" to "Gold Titanium",
  "ocean_glass" to "Ocean Glass",
  "gs_premium" to "GS Premium")
 fun install(context:Context){
  val prefs=context.getSharedPreferences("studio_backgrounds",0)
  for((name,title) in names){
   val file=File(context.filesDir,"scene-premium-$name.png")
   if(file.exists())continue
   val id=context.resources.getIdentifier("premium_$name","drawable",context.packageName)
   val drawable=context.getDrawable(id)?:continue
   val image=Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888)
   try{drawable.setBounds(0,0,976,596);drawable.draw(Canvas(image));val temporary=File(context.filesDir,file.name+".tmp")
    temporary.outputStream().use{check(image.compress(Bitmap.CompressFormat.PNG,100,it))};check(temporary.renameTo(file))
    prefs.edit().putString("name:${file.name}",title).apply()
   }finally{image.recycle()}
  }
 }
}
