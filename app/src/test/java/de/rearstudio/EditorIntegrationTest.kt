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
 @Test fun editorCanOpenWithSavedDesignWithoutCommittingADraft(){val c=ApplicationProvider.getApplicationContext<Context>();val original=StudioScene.defaults();StudioScene.save(c,original);val before=c.getSharedPreferences("studio",0).getString("scene",null);ActivityScenario.launch(EditorActivity::class.java).use{};assertEquals(before,c.getSharedPreferences("studio",0).getString("scene",null))}
 @Test fun dragMovesNormalizedWidgetInsidePanel(){val c=ApplicationProvider.getApplicationContext<Context>();val view=StudioCanvas(c);view.editing=true;view.scene=StudioScene.defaults();view.layout(0,0,976,596);val w=view.scene.profile("normal").widgets[0];val x=(w.x+w.w/2)*976;val y=(w.y+w.h/2)*596;val oldX=w.x;fun event(a:Int,xx:Float,yy:Float){val e=MotionEvent.obtain(0,1,a,xx,yy,0);view.onTouchEvent(e);e.recycle()};event(MotionEvent.ACTION_DOWN,x,y);event(MotionEvent.ACTION_MOVE,x+35,y+25);event(MotionEvent.ACTION_UP,x+35,y+25);assertTrue(w.x>oldX);assertTrue(w.x+w.w<=1);view.dispose()}
 @Test fun notificationBannerDispatchesTheActualKey(){val c=ApplicationProvider.getApplicationContext<Context>();val layer=NotificationLayer(c);var key:String?=null;layer.onOpen={key=it};layer.showBanner(ShownNotification("notification-key","example.app","Example","Title","Text",0),0);assertTrue(layer.getChildAt(0).performClick());assertEquals("notification-key",key)}
 @Test fun expiredEditorPreviewDoesNotReplaceSavedScene(){val c=ApplicationProvider.getApplicationContext<Context>();val saved=StudioScene.defaults();StudioScene.save(c,saved);val draft=saved.copy();draft.accent=0xffff0000.toInt();c.getSharedPreferences("studio",0).edit().putString("preview",draft.json()).putLong("preview_time",System.currentTimeMillis()-30_000).commit();assertEquals(saved.accent,StudioScene.load(c,true).accent)}
 @Test fun allThreeLanguagesProvideEditorLabels(){val c=ApplicationProvider.getApplicationContext<Context>();for(lang in listOf("de","en","tr")){val config=android.content.res.Configuration(c.resources.configuration).apply{setLocale(java.util.Locale.forLanguageTag(lang))};val localized=c.createConfigurationContext(config);assertNotEquals("your_design",StudioUi.text(localized,"your_design"));assertNotEquals("charging_display",StudioUi.text(localized,"charging_display"))}}
 @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 @Test fun cameraGuidesDoNotChangeBackgroundOpacityOnLaterFrames(){val c=ApplicationProvider.getApplicationContext<Context>();val view=StudioCanvas(c);view.cameraGuides=true;view.scene=StudioScene.defaults().apply{profile("normal").widgets.clear()};view.layout(0,0,976,596);fun frame():Int{val bitmap=android.graphics.Bitmap.createBitmap(976,596,android.graphics.Bitmap.Config.ARGB_8888);view.draw(android.graphics.Canvas(bitmap));return bitmap.getPixel(500,550).also{bitmap.recycle()}};val first=frame();assertEquals(255,android.graphics.Color.alpha(first));assertEquals(first,frame());view.dispose()}
 @Test fun keepingNormalLayoutStillAllowsChargingWidgetDragAndResize(){
  val c=ApplicationProvider.getApplicationContext<Context>();val view=StudioCanvas(c);view.editing=true;view.forcedProfile="charging";view.scene=StudioScene.defaults();view.layout(0,0,976,596)
  val profile=view.scene.profile("charging");profile.reuseNormal=true;profile.chargeStyle="ring";val battery=profile.widgets.first{it.kind=="battery"};val originalNormal=view.scene.profile("normal").json().toString()
  fun event(a:Int,x:Float,y:Float){val e=MotionEvent.obtain(0,1,a,x,y,0);view.onTouchEvent(e);e.recycle()}
  val x=(battery.x+battery.w/2)*976;val y=(battery.y+battery.h/2)*596;val oldX=battery.x
  event(MotionEvent.ACTION_DOWN,x,y);event(MotionEvent.ACTION_MOVE,x+30,y+20);event(MotionEvent.ACTION_UP,x+30,y+20);assertEquals(battery.id,view.selected);assertTrue(battery.x>oldX)
  val right=(battery.x+battery.w)*976;val bottom=(battery.y+battery.h)*596;val oldWidth=battery.w
  event(MotionEvent.ACTION_DOWN,right,bottom);event(MotionEvent.ACTION_MOVE,right+20,bottom+15);event(MotionEvent.ACTION_UP,right+20,bottom+15);assertTrue(battery.w>oldWidth);assertEquals(originalNormal,view.scene.profile("normal").json().toString());view.dispose()
 }
}
