package de.rearstudio
import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import de.rearstudio.notifications.*
@RunWith(AndroidJUnit4::class)
class EditorIntegrationTest {
 @Test fun editorCanOpenWithSavedDesignWithoutCommittingADraft(){val c=ApplicationProvider.getApplicationContext<Context>();val original=StudioScene.defaults();StudioScene.save(c,original);ActivityScenario.launch(EditorActivity::class.java).use{};assertEquals(original.json(),StudioScene.load(c).json())}
 @Test fun dragMovesNormalizedWidgetInsidePanel(){val c=ApplicationProvider.getApplicationContext<Context>();val view=StudioCanvas(c);view.editing=true;view.scene=StudioScene.defaults();view.layout(0,0,976,596);val w=view.scene.profile("normal").widgets[0];val x=(w.x+w.w/2)*976;val y=(w.y+w.h/2)*596;val oldX=w.x;fun event(a:Int,xx:Float,yy:Float){val e=MotionEvent.obtain(0,1,a,xx,yy,0);view.onTouchEvent(e);e.recycle()};event(MotionEvent.ACTION_DOWN,x,y);event(MotionEvent.ACTION_MOVE,x+35,y+25);event(MotionEvent.ACTION_UP,x+35,y+25);assertTrue(w.x>oldX);assertTrue(w.x+w.w<=1);view.dispose()}
 @Test fun draggingAcrossViewportDoesNotClampAndSaveKeepsPosition(){
  val c=ApplicationProvider.getApplicationContext<Context>();val view=StudioCanvas(c);view.editing=true;view.scene=StudioScene.defaults();view.layout(0,0,976,596)
  val w=view.scene.profile("normal").widgets[0];val x=(w.x+w.w/2)*976;val y=(w.y+w.h/2)*596
  fun event(a:Int,xx:Float,yy:Float){val e=MotionEvent.obtain(0,1,a,xx,yy,0);view.onTouchEvent(e);e.recycle()}
  event(MotionEvent.ACTION_DOWN,x,y);event(MotionEvent.ACTION_MOVE,x-976,y+596);event(MotionEvent.ACTION_UP,x-976,y+596)
  assertEquals(-.65f,w.x,.0001f);assertEquals(1.1f,w.y,.0001f);StudioScene.save(c,view.scene)
  val saved=StudioScene.load(c).profile("normal").widgets[0];assertEquals(w.x,saved.x,0f);assertEquals(w.y,saved.y,0f);view.dispose()
 }
 @Test fun resizeWorksWhenWidgetIsAlreadyBeyondRightEdge(){
  val c=ApplicationProvider.getApplicationContext<Context>();val view=StudioCanvas(c);view.editing=true;view.scene=StudioScene.defaults();view.layout(0,0,976,596)
  val w=view.scene.profile("normal").widgets[0];w.x=1.2f;view.selected=w.id;val x=(w.x+w.w)*976;val y=(w.y+w.h)*596
  fun event(a:Int,xx:Float,yy:Float){val e=MotionEvent.obtain(0,1,a,xx,yy,0);view.onTouchEvent(e);e.recycle()}
  event(MotionEvent.ACTION_DOWN,x,y);event(MotionEvent.ACTION_MOVE,x+976,y+596);event(MotionEvent.ACTION_UP,x+976,y+596)
  assertEquals(1.58f,w.w,.0001f);assertEquals(1.32f,w.h,.0001f);assertEquals(1.2f,w.x,0f);view.dispose()
 }
 @Test fun notificationBannerDispatchesTheActualKey(){val c=ApplicationProvider.getApplicationContext<Context>();val layer=NotificationLayer(c);var key:String?=null;layer.onOpen={key=it};layer.showBanner(ShownNotification("notification-key","example.app","Example","Title","Text",0),0);assertTrue(layer.getChildAt(0).performClick());assertEquals("notification-key",key)}
 @Test fun expiredEditorPreviewDoesNotReplaceSavedScene(){val c=ApplicationProvider.getApplicationContext<Context>();val saved=StudioScene.defaults();StudioScene.save(c,saved);val draft=saved.copy();draft.accent=0xffff0000.toInt();c.getSharedPreferences("studio",0).edit().putString("preview",draft.json()).putLong("preview_time",System.currentTimeMillis()-30_000).commit();assertEquals(saved.accent,StudioScene.load(c,true).accent)}
 @Test fun allThreeLanguagesProvideEditorLabels(){val c=ApplicationProvider.getApplicationContext<Context>();for(lang in listOf("de","en","tr")){val config=android.content.res.Configuration(c.resources.configuration).apply{setLocale(java.util.Locale.forLanguageTag(lang))};val localized=c.createConfigurationContext(config);assertNotEquals("your_design",StudioUi.text(localized,"your_design"));assertNotEquals("charging_display",StudioUi.text(localized,"charging_display"))}}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test fun cameraGuidesDoNotChangeBackgroundOpacityOnLaterFrames(){val c=ApplicationProvider.getApplicationContext<Context>();val view=StudioCanvas(c);view.cameraGuides=true;view.scene=StudioScene.defaults().apply{profile("normal").widgets.clear()};view.layout(0,0,976,596);fun frame():Int{val bitmap=android.graphics.Bitmap.createBitmap(976,596,android.graphics.Bitmap.Config.ARGB_8888);view.draw(android.graphics.Canvas(bitmap));return bitmap.getPixel(500,550).also{bitmap.recycle()}};val first=frame();assertEquals(255,android.graphics.Color.alpha(first));assertEquals(first,frame());view.dispose()}
}
