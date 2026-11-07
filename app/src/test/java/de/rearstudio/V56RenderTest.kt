package de.rearstudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class V56RenderTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun render(view:StudioCanvas):Bitmap=Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888).also{view.layout(0,0,976,596);view.draw(Canvas(it))}
    @Test fun calibratedLensesExcludeWidgetPixels(){
        val view=StudioCanvas(context);val profile=view.scene.profile("normal");profile.widgets.clear();profile.background="solid";profile.color=Color.BLACK;profile.dim=0
        view.scene.geometry=DisplayGeometry(lenses=listOf(LensCircle(120f,150f,60f),LensCircle(120f,440f,60f)))
        profile.widgets.add(StudioWidget(kind="gradient",x=0f,y=0f,w=1f,h=1f,color=Color.RED).apply{options.put("color2",Color.RED)})
        val bitmap=render(view);assertEquals(Color.BLACK,bitmap.getPixel(120,150));assertEquals(Color.BLACK,bitmap.getPixel(120,440));assertEquals(Color.RED,bitmap.getPixel(500,300));bitmap.recycle();view.dispose()
    }
    @Test fun translucentCardsPreserveUnderlyingBackground(){
        val view=StudioCanvas(context);val profile=view.scene.profile("normal");profile.widgets.clear();profile.background="solid";profile.color=Color.BLACK;profile.dim=0
        profile.widgets.add(StudioWidget(kind="notification",style="liquid",x=.35f,y=.1f,w=.6f,h=.8f))
        val bitmap=render(view);val pixel=bitmap.getPixel(850,200)
        assertTrue(Color.red(pixel) in 1..42);assertTrue(Color.blue(pixel) in 1..55);assertEquals(255,Color.alpha(pixel));bitmap.recycle();view.dispose()
    }
    @Test fun multiplyBlendComposesAgainstBackground(){
        val view=StudioCanvas(context);val profile=view.scene.profile("normal");profile.widgets.clear();profile.background="solid";profile.color=Color.BLUE;profile.dim=0
        profile.widgets.add(StudioWidget(kind="gradient",x=.35f,y=.1f,w=.6f,h=.8f,color=Color.RED).apply{options.put("color2",Color.RED).put("blend","multiply")})
        val bitmap=render(view);assertEquals(Color.BLACK,bitmap.getPixel(500,300));bitmap.recycle();view.dispose()
    }
    @Test fun lockedWidgetCannotBeDragged(){
        val view=StudioCanvas(context).apply{editing=true};view.layout(0,0,976,596);val w=view.scene.profile("normal").widgets.first();w.locked=true
        val x=(w.x+w.w/2)*976;val y=(w.y+w.h/2)*596;val before=w.json().toString()
        for((action,dx) in listOf(MotionEvent.ACTION_DOWN to 0f,MotionEvent.ACTION_MOVE to 150f,MotionEvent.ACTION_UP to 150f)){
            val event=MotionEvent.obtain(0,10,action,x+dx,y,0);view.onTouchEvent(event);event.recycle()
        }
        assertEquals(before,w.json().toString());view.dispose()
    }
    @Test fun groupedDragPreservesRelativePositionsAndLockedMembers(){
        val view=StudioCanvas(context).apply{editing=true};view.layout(0,0,976,596)
        val widgets=view.scene.profile("normal").widgets;widgets.clear()
        val a=StudioWidget(kind="text",x=.4f,y=.2f,w=.2f,h=.2f).apply{options.put("group","g")}
        val b=StudioWidget(kind="text",x=.7f,y=.2f,w=.2f,h=.2f).apply{options.put("group","g")};widgets.addAll(listOf(a,b))
        val difference=b.x-a.x
        for((action,offset) in listOf(MotionEvent.ACTION_DOWN to 0f,MotionEvent.ACTION_MOVE to 40f,MotionEvent.ACTION_UP to 40f)){
            val e=MotionEvent.obtain(0,10,action,450f+offset,150f,0);view.onTouchEvent(e);e.recycle()
        }
        assertTrue(a.x>.4f);assertEquals(difference,b.x-a.x,.001f);view.dispose()
    }
    @Test fun cameraLightNeverPaintsInsideLenses(){
        val geometry=DisplayGeometry(lenses=listOf(LensCircle(120f,150f,60f),LensCircle(120f,440f,60f)))
        val bitmap=Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap);canvas.drawColor(Color.BLACK)
        CameraLightRenderer.draw(canvas,geometry,976,596,LightAnimation(enabled=true,brightness=100,fadeMs=0),1000,false)
        assertEquals(Color.BLACK,bitmap.getPixel(120,150));assertEquals(Color.BLACK,bitmap.getPixel(179,150))
        bitmap.recycle()
    }
    @Test fun unknownGeometryProducesNoCameraLight(){
        val bitmap=Bitmap.createBitmap(976,596,Bitmap.Config.ARGB_8888);val canvas=Canvas(bitmap)
        CameraLightRenderer.draw(canvas,DisplayGeometry(),976,596,LightAnimation(enabled=true,fadeMs=0),1000,false)
        assertEquals(0,bitmap.getPixel(120,150));bitmap.recycle()
    }
    @Test fun inactiveRendererClosesTransientViewAndPausesAnimation(){
        val view=StudioCanvas(context);view.openSmartView("smart_music");view.setRuntimeActive(false)
        assertNull(view.smartViews.opened);assertFalse(view.runtimeActive);assertFalse(view.previewPlaying)
        view.setRuntimeActive(true);assertTrue(view.runtimeActive);assertTrue(view.previewPlaying);view.dispose()
    }
    @Test fun saveExtendedRendererFixtures(){
        val c=context.getSharedPreferences("activity_steps",0);c.edit().putLong("day:"+java.time.LocalDate.now(),4230).commit()
        val weather=context.getSharedPreferences("studio_weather",0);weather.edit().putString("response","""{"current":{"temperature_2m":12.4,"relative_humidity_2m":72,"wind_speed_10m":8.1,"weather_code":45,"time":"2026-11-07T13:30"}}""").commit()
        try{for(kind in listOf("activity","weather","rotation_widget")){
            val view=StudioCanvas(context);val profile=view.scene.profile("normal");profile.widgets.clear();profile.background="solid";profile.color=0xff101116.toInt();profile.dim=0
            profile.widgets.add(StudioWidget(kind=kind,x=.35f,y=.06f,w=.61f,h=.89f,style="liquid",color=0xffe6bd77.toInt()).apply{options.put("font","Inter")})
            val bitmap=render(view);val output=File("build/reports/studio-$kind.png");output.parentFile?.mkdirs();output.outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle();view.dispose()
        }}finally{c.edit().clear().commit();weather.edit().clear().commit()}
    }
    @Test fun saveRendererPreviewForVisualReview(){
        val view=StudioCanvas(context);val p=view.scene.profile("normal");p.widgets.clear();p.background="solid";p.color=0xff101116.toInt();p.dim=0
        p.widgets.add(StudioWidget(kind="gradient",x=.35f,y=.06f,w=.59f,h=.86f,color=0xff191c24.toInt()).apply{options.put("color2",0xff3f2635.toInt())})
        p.widgets.add(StudioWidget(kind="text",x=.4f,y=.17f,w=.5f,h=.13f,color=0xffefeff3.toInt()).apply{options.put("text","AHM3T45 Studio")})
        p.widgets.add(StudioWidget(kind="text",x=.4f,y=.35f,w=.5f,h=.24f,color=0xffe6bd77.toInt()).apply{options.put("text","12:45")})
        p.widgets.add(StudioWidget(kind="text",x=.4f,y=.69f,w=.5f,h=.10f).apply{options.put("text","V5.6 · Renderer test")})
        val bitmap=render(view);val output=File("build/reports/studio-preview.png");output.parentFile.mkdirs();output.outputStream().use{assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))};bitmap.recycle();view.dispose()
    }
}
