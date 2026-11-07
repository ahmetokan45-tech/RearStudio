package de.rearstudio
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class NotificationAppearanceTest {
 @Test fun overridesStayPerAppAndDoNotModifySavedWidget(){val c=ApplicationProvider.getApplicationContext<Context>();val w=StudioWidget(kind="notification",style="card",color=123);NotificationAppearance.save(c,"test.one","liquid",456);val changed=NotificationAppearance.apply(c,w,"test.one");assertEquals("liquid",changed.style);assertEquals(456,changed.color);assertEquals(w,NotificationAppearance.apply(c,w,"test.two"));assertEquals("card",w.style);NotificationAppearance.clear(c,"test.one");assertEquals(w,NotificationAppearance.apply(c,w,"test.one"))}
}
