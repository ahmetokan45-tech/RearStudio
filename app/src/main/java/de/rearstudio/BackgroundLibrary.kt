package de.rearstudio

import android.content.Context
import android.graphics.BitmapFactory
import java.io.File

/** The original imported file is private, persistent, and never rewritten by image adjustments. */
class BackgroundLibrary(private val context:Context) {
    init{PremiumBackgrounds.install(context);UserArtwork.install(context)}
    data class Entry(val file:File,val name:String,val favorite:Boolean,val usedAt:Long)
    private val prefs=context.getSharedPreferences("studio_backgrounds",0)
    fun list():List<Entry> = context.filesDir.listFiles().orEmpty().filter{it.isFile&&it.name.startsWith("scene-")}.mapNotNull{file->
        val options=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.absolutePath,options)
        if(options.outWidth<=0||options.outHeight<=0)null else Entry(file,prefs.getString("name:${file.name}","Image ${file.name.takeLast(8)}")?:file.name,prefs.getBoolean("favorite:${file.name}",false),prefs.getLong("used:${file.name}",0))
    }.sortedWith(compareByDescending<Entry>{it.favorite}.thenByDescending{it.usedAt})
    fun references(file:File,additional:StudioScene?=null):Boolean {
        val path=file.canonicalPath
        fun contains(scene:StudioScene)=scene.profiles.values.any{it.photo.isNotBlank()&&runCatching{File(it.photo).canonicalPath==path}.getOrDefault(false)}
        if(additional?.let(::contains)==true||contains(StudioScene.load(context)))return true
        val studio=context.getSharedPreferences("studio",0)
        for(key in listOf("draft","preview","previous_design","previous_scene")){
            val scene=studio.getString(key,null)?.let{runCatching{StudioScene.parse(it)}.getOrNull()}
            if(scene?.let(::contains)==true)return true
        }
        return DesignLibrary(context).list().any{contains(StudioScene.parse(it.scene))}
    }
    fun delete(entry:Entry,additional:StudioScene?=null):Boolean {
        if(entry.file.name.startsWith("scene-premium-")||entry.file.canonicalFile.parentFile!=context.filesDir.canonicalFile||references(entry.file,additional))return false
        if(!entry.file.delete())return false
        prefs.edit().remove("name:${entry.file.name}").remove("favorite:${entry.file.name}").remove("used:${entry.file.name}").apply();return true
    }
    fun rename(entry:Entry,name:String){require(name.isNotBlank());prefs.edit().putString("name:${entry.file.name}",name.trim().take(80)).apply()}
    fun favorite(entry:Entry){prefs.edit().putBoolean("favorite:${entry.file.name}",!entry.favorite).apply()}
    fun touch(file:File){prefs.edit().putLong("used:${file.name}",System.currentTimeMillis()).apply()}
}
