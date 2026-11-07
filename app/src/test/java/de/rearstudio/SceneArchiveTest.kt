package de.rearstudio
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*
import java.util.zip.*
@RunWith(AndroidJUnit4::class)
class SceneArchiveTest {
 private val context=ApplicationProvider.getApplicationContext<Context>()
 private fun zip(vararg entries:Pair<String,ByteArray>):ByteArray=ByteArrayOutputStream().also{out->ZipOutputStream(out).use{z->entries.forEach{(k,v)->z.putNextEntry(ZipEntry(k));z.write(v);z.closeEntry()}}}.toByteArray()
 @Test fun portableImagesAndGeometryIsolation(){
  val image=File(context.filesDir,"scene-archive-test");Bitmap.createBitmap(4,4,Bitmap.Config.ARGB_8888).let{b->image.outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
  val source=StudioScene.defaults();source.profile("normal").photo=image.path;source.profile("normal").background="photo";source.profile("charging").photo=image.path
  source.geometry=DisplayGeometry(lenses=listOf(LensCircle(50f,50f,20f)))
  val current=StudioScene.defaults().apply{geometry=DisplayGeometry(lenses=listOf(LensCircle(100f,100f,40f)))}
  val out=ByteArrayOutputStream();SceneArchive.export(context,source,out)
  val imported=SceneArchive.import(context,ByteArrayInputStream(out.toByteArray()),current)
  assertEquals(current.geometry,imported.geometry);assertNotEquals(image.path,imported.profile("normal").photo);assertEquals(imported.profile("normal").photo,imported.profile("charging").photo)
  assertArrayEquals(image.readBytes(),File(imported.profile("normal").photo).readBytes());assertEquals(image.path,source.profile("normal").photo)
 }
 @Test fun traversalAndExternalReferencesRejected(){
  assertThrows(IllegalArgumentException::class.java){SceneArchive.import(context,ByteArrayInputStream(zip("../scene.json" to byteArrayOf(0))),StudioScene.defaults())}
  val scene=StudioScene.defaults().apply{profile("normal").photo="/etc/passwd"}
  assertThrows(IllegalArgumentException::class.java){SceneArchive.import(context,ByteArrayInputStream(zip("scene.json" to scene.json().toByteArray())),StudioScene.defaults())}
 }
 @Test fun invalidImagesRejectedWithoutPersistentFiles(){
  val before=context.filesDir.listFiles().orEmpty().map{it.name}.toSet()
  val scene=StudioScene.defaults().apply{profile("normal").photo="images/0"}
  assertThrows(IllegalArgumentException::class.java){SceneArchive.import(context,ByteArrayInputStream(zip("scene.json" to scene.json().toByteArray(),"images/0" to byteArrayOf(0,1,2))),StudioScene.defaults())}
  assertEquals(before,context.filesDir.listFiles().orEmpty().map{it.name}.toSet())
 }
}
