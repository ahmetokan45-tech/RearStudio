package de.rearstudio

import android.content.Context
import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.*
import java.util.UUID
import java.util.zip.*

/** Portable design with private image copies. Imported paths are never trusted. */
object SceneArchive {
    private const val MAX_BYTES=80*1024*1024
    private const val MAX_SCENE=2*1024*1024
    fun export(context:Context, source:StudioScene, output:OutputStream) {
        val scene=source.copy()
        val images=linkedMapOf<String,File>()
        scene.profiles.values.forEach { profile ->
            if(profile.photo.isNotBlank()) {
                val file=File(profile.photo).canonicalFile
                require(file.parentFile==context.filesDir.canonicalFile && file.isFile) { "Missing private image" }
                val key=images.entries.firstOrNull{it.value==file}?.key ?: "images/${images.size}".also{images[it]=file}
                profile.photo=key
            }
        }
        scene.geometry=null // Device calibration never travels in a design archive.
        val raw=scene.json().toByteArray(Charsets.UTF_8)
        require(raw.size<=MAX_SCENE && images.values.sumOf{it.length()}+raw.size<=MAX_BYTES)
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("scene.json"));zip.write(raw);zip.closeEntry()
            images.forEach{(name,file)->zip.putNextEntry(ZipEntry(name));file.inputStream().use{it.copyTo(zip)};zip.closeEntry()}
        }
    }
    fun import(context:Context,input:InputStream,current:StudioScene):StudioScene {
        val files=linkedMapOf<String,File>();var raw:ByteArray?=null;var total=0L
        val staged=File(context.cacheDir,"design-import-${UUID.randomUUID()}").apply{check(mkdirs())}
        val committed=mutableListOf<File>()
        try {
            ZipInputStream(input).use{zip->
                var count=0
                while(true){val entry=zip.nextEntry?:break;require(++count<=65)
                    val name=entry.name
                    require(!entry.isDirectory && (name=="scene.json" || name.matches(Regex("images/[0-9]{1,2}"))))
                    require(if(name=="scene.json")raw==null else !files.containsKey(name))
                    val target=File(staged,"entry-$count");var length=0
                    target.outputStream().use{out->val buffer=ByteArray(8192);while(true){val n=zip.read(buffer);if(n<0)break;total+=n;length+=n;require(total<=MAX_BYTES);if(name=="scene.json")require(length<=MAX_SCENE);out.write(buffer,0,n)}}
                    if(name=="scene.json")raw=target.readBytes() else files[name]=target
                    zip.closeEntry()
                }
            }
            val scene=StudioScene.parse(requireNotNull(raw).toString(Charsets.UTF_8))
            val referenced=scene.profiles.values.map{it.photo}.filter{it.isNotBlank()}.toSet()
            require(referenced==files.keys)
            val mapping=files.mapValues{(_,file)->
                val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true};BitmapFactory.decodeFile(file.path,bounds)
                require(bounds.outWidth in 1..16384 && bounds.outHeight in 1..16384)
                val dest=File(context.filesDir,"scene-${UUID.randomUUID()}")
                committed.add(dest);file.copyTo(dest);dest.absolutePath
            }
            scene.profiles.values.forEach{if(it.photo.isNotBlank())it.photo=mapping.getValue(it.photo)}
            return DesignLibrary.compose(current,scene,false)
        } catch(e:Exception){committed.forEach{it.delete()};throw e} finally {staged.deleteRecursively()}
    }
}
