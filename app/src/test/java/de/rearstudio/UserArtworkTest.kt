package de.rearstudio

import android.content.Context
import android.graphics.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UserArtworkTest {
 private val context=ApplicationProvider.getApplicationContext<Context>()
 @Test fun backgroundsAreInstalledWithoutReencoding(){
  UserArtwork.install(context)
  assertEquals(9,UserArtwork.backgrounds.size)
  for(id in UserArtwork.backgrounds.keys){
   val original=context.assets.open("studio/$id.jpg").use{it.readBytes()}
   val installed=File(context.filesDir,"scene-premium-user-$id.jpg")
   assertArrayEquals(original,installed.readBytes())
   assertTrue(BackgroundLibrary(context).list().any{it.file==installed})
  }
 }
 @Test fun overlaysPreserveSourcePixelsAtReferenceSize(){
  assertEquals(6,UserArtwork.overlays.keys.count{it.isNotEmpty()})
  for(id in UserArtwork.overlays.keys.filter{it.isNotEmpty()}){
   val source=context.assets.open("studio/$id.png").use{BitmapFactory.decodeStream(it)}!!
   assertEquals(976,source.width);assertEquals(596,source.height)
   val rendered=Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888)
   UserArtwork.Renderer(context).draw(Canvas(rendered),id,976,596)
   assertTrue("Original pixels: $id",source.sameAs(rendered))
   source.recycle();rendered.recycle()
  }
 }
 @Test fun fixedAspectRatioAndValidatedPersistence(){
  assertEquals(RectF(0f,0f,976f,596f),UserArtwork.bounds(976,596))
  val bounds=UserArtwork.bounds(1200,596)
  assertEquals(976f,bounds.width(),0f);assertEquals(112f,bounds.left,0f)
  val scene=StudioScene.defaults();scene.profile("normal").artworkOverlay="102142"
  assertEquals("102142",StudioScene.parse(scene.json().toString()).profile("normal").artworkOverlay)
  assertEquals("",UserArtwork.validOverlay("../../external.png"))
  assertEquals("",UserArtwork.validOverlay("102281"))
 }
 @Test fun switchingOffClearsPreviousOverlay(){
  val renderer=UserArtwork.Renderer(context)
  renderer.draw(Canvas(Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888)),"102142",976,596)
  val blank=Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888)
  renderer.draw(Canvas(blank),"",976,596)
  assertTrue(blank.sameAs(Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888)))
 }
}
