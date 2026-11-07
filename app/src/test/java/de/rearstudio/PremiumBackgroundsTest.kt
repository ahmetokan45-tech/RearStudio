package de.rearstudio
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PremiumBackgroundsTest {
 private val context=ApplicationProvider.getApplicationContext<Context>()
 @Test fun tenDistinctOfflineOriginals(){PremiumBackgrounds.install(context);val files=PremiumBackgrounds.names.keys.map{File(context.filesDir,"scene-premium-$it.png")};assertEquals(10,files.size);assertTrue(files.all{it.length()>100});val hashes=files.map{java.security.MessageDigest.getInstance("SHA-256").digest(it.readBytes()).toList()};assertEquals(10,hashes.distinct().size)}
 @Test fun savedAndDraftReferencesPreventDeletion(){
  val library=BackgroundLibrary(context);val original=library.list().first();val file=File(context.filesDir,"scene-delete-test");original.file.copyTo(file,true)
  val entry=library.list().first{it.file==file};val scene=StudioScene.defaults().apply{profile("normal").photo=file.path}
  assertFalse(library.delete(entry,scene));assertTrue(file.exists())
  val saved=DesignLibrary(context).save("Uses image",scene);assertFalse(library.delete(entry));DesignLibrary(context).delete(saved.id)
  assertTrue(library.delete(entry));assertFalse(file.exists())
 }
 @Test fun bundledFontsDecode(){for(name in StudioFonts.families.keys){assertNotNull(StudioFonts.get(context,name,0))};assertEquals(12,StudioFonts.families.size)}
}
