package de.rearstudio

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StudioIntegrationTest {
    @Test fun importedRearPolicyAndServiceArePackagedTogether() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager
        val app = pm.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        assertEquals(1, app.metaData.getInt("miui.rear.policy"))
        val host = pm.getActivityInfo(ComponentName(context, de.rearstudio.rear.RearHostActivity::class.java), PackageManager.GET_META_DATA)
        assertEquals(1, host.metaData.getInt("miui.rear.policy"))
        assertTrue(host.exported)
        val service = pm.getServiceInfo(ComponentName(context, de.rearstudio.core.KeeperService::class.java), 0)
        assertFalse(service.exported)
        assertFalse(pm.getActivityInfo(ComponentName(context, DisplayActivity::class.java), 0).exported)
    }
    @Test fun studioLauncherStartsWithImportedApplicationAndShowsDesignEntry() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                fun texts(view: View): List<String> = (if (view is TextView) listOf(view.text.toString()) else emptyList()) +
                    (if (view is ViewGroup) (0 until view.childCount).flatMap { texts(view.getChildAt(it)) } else emptyList())
                val values = texts(activity.window.decorView)
                assertTrue(values.contains("AHM3T45"))
                assertTrue(values.contains("Hintergründe, Uhr & Ladeanzeigen"))
            }
        }
    }
}
