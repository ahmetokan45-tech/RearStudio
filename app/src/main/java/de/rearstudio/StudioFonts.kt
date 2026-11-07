package de.rearstudio
import android.content.Context
import android.graphics.Typeface
object StudioFonts {
 val families=linkedMapOf("Anton" to "anton", "Rubik" to "rubik", "Roboto" to "roboto", "Inter" to "inter", "Montserrat" to "montserrat", "Poppins" to "poppins", "Orbitron" to "orbitron", "Rajdhani" to "rajdhani", "Oswald" to "oswald", "Exo 2" to "exo2", "Bebas Neue" to "bebasneue", "Manrope" to "manrope")
 private val cache=mutableMapOf<String,Typeface>()
 fun get(context:Context,family:String,style:Int):Typeface {
  val base=cache.getOrPut(family){families[family]?.let{runCatching{Typeface.createFromAsset(context.assets,"fonts/$it.ttf")}.getOrNull()}?:Typeface.create(family,Typeface.NORMAL)}
  return Typeface.create(base,style)
 }
}
