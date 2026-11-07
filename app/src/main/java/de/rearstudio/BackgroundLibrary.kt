package de.rearstudio

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File

/** The original imported file is private, persistent, and never rewritten by image adjustments. */
class BackgroundLibrary(private val context:Context) {
    data class Entry(val file:File,val name:String,val favorite:Boolean,val usedAt:Long)
    private val prefs=context.getSharedPreferences("studio_backgrounds",0)
    fun list():List<Entry> = context.filesDir.listFiles().orEmpty().filter{it.isFile&&it.name.startsWith("scene-")}.mapNotNull{file->
        val options=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.absolutePath,options)
        if(options.outWidth<=0||options.outHeight<=0)null else Entry(file,prefs.getString("name:${file.name}","Image ${file.name.takeLast(8)}")?:file.name,prefs.getBoolean("favorite:${file.name}",false),prefs.getLong("used:${file.name}",0))
    }.sortedWith(compareByDescending<Entry>{it.favorite}.thenByDescending{it.usedAt})
    fun rename(entry:Entry,name:String){require(name.isNotBlank());prefs.edit().putString("name:${entry.file.name}",name.trim().take(80)).apply()}
    fun favorite(entry:Entry){prefs.edit().putBoolean("favorite:${entry.file.name}",!entry.favorite).apply()}
    fun touch(file:File){prefs.edit().putLong("used:${file.name}",System.currentTimeMillis()).apply()}
}
