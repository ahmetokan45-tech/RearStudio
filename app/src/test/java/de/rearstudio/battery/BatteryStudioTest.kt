package de.rearstudio.battery

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BatteryStudioTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() {
        context.getSharedPreferences("battery_studio_v1",0).edit().clear().commit()
    }
    @Test fun profilesRoundTripAndOnlyActivationChangesTheRenderer() {
        BatterySettings.setStyle(context,ChargingStyle.RING)
        val p=BatteryProfile("custom","Night",ChargingStyle.MINIMAL,0xff123456.toInt(),false,true,false,true)
        BatteryProfiles.save(context,listOf(p))
        assertEquals(listOf(p),BatteryProfiles.load(context))
        assertEquals(ChargingStyle.RING,BatterySettings.style(context))
        BatteryProfiles.activate(context,p)
        assertEquals(p.id,BatteryProfiles.activeId(context))
        assertEquals(p.style,BatterySettings.style(context))
        assertEquals(p.color,BatterySettings.color(context))
        assertFalse(BatterySettings.connectedEvent(context))
        assertTrue(BatterySettings.fullEvent(context))
        assertFalse(BatterySettings.lightUp(context))
        assertTrue(BatterySettings.overXiaomi(context))
    }
    @Test fun fullEventFiresOnceAndResetsAfterDisconnect() {
        val gate=FullChargeGate()
        assertFalse(gate.update(true,false))
        assertTrue(gate.update(true,true))
        assertFalse(gate.update(true,true))
        assertFalse(gate.update(true,false))
        assertFalse(gate.update(true,true))
        assertFalse(gate.update(false,false))
        assertFalse(gate.update(true,false))
        assertTrue(gate.update(true,true))
    }
    @Test fun startingAtFullDoesNotReplayAChargeEvent() {
        val gate=FullChargeGate()
        assertFalse(gate.update(true,true))
        assertFalse(gate.update(true,true))
    }
    @Test fun batteryBroadcastUpdatesAndFullEventReachMonitor() {
        BatterySettings.setEnabled(context,true)
        BatterySettings.setEvents(context,false,true)
        val played=mutableListOf<ChargingStatus>()
        val updated=mutableListOf<ChargingStatus>()
        val monitor=ChargingMonitor(context,{ played.add(it) },{},{},{ updated.add(it) })
        monitor.start()
        try {
            fun send(level: Int) {
                context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_BATTERY_CHANGED)
                    .putExtra(android.os.BatteryManager.EXTRA_LEVEL,level)
                    .putExtra(android.os.BatteryManager.EXTRA_SCALE,100)
                    .putExtra(android.os.BatteryManager.EXTRA_PLUGGED,1))
                org.robolectric.shadows.ShadowLooper.idleMainLooper()
            }
            send(99); send(100); send(100)
            assertEquals(listOf(100),played.map { it.level })
            assertTrue(updated.any { it.level==99 })
            assertTrue(updated.last().full)
        } finally { monitor.stop() }
    }
    @Test fun studioSceneStillIncludesAPlayableChargingOverlay() {
        context.getSharedPreferences("studio",0).edit().putBoolean("scene_enabled",true).commit()
        val id=org.robolectric.shadows.ShadowDisplayManager.addDisplay("w976dp-h596dp-land-mdpi")
        val display=context.getSystemService(android.hardware.display.DisplayManager::class.java).getDisplay(id)
        val controller=org.robolectric.Robolectric.buildActivity(de.rearstudio.rear.RearHostActivity::class.java).create()
        try {
            val host=controller.get()
            val root=android.widget.FrameLayout(host)
            val add=host.javaClass.getDeclaredMethod("addCharging",android.widget.FrameLayout::class.java,android.view.Display::class.java)
            add.isAccessible=true;add.invoke(host,root,display)
            assertEquals(1,root.childCount)
            val layer=root.getChildAt(0) as ChargingLayer
            layer.play(ChargingStatus(72))
            assertTrue(layer.isPlaying)
            layer.clear()
        } finally { controller.destroy() }
    }
    private fun texts(v: View): List<TextView> = (if(v is TextView) listOf(v) else emptyList()) +
        (if(v is ViewGroup) (0 until v.childCount).flatMap { texts(v.getChildAt(it)) } else emptyList())
    @Test fun editorTabsAndDraftSurviveRecreationWithoutActivation() {
        ActivityScenario.launch(BatteryStudioActivity::class.java).use { scenario ->
            scenario.onActivity { a ->
                texts(a.window.decorView).first { it.text.toString()=="Bearbeiten" }.performClick()
                listOf("Design","Ereignisse","Effekte","Vorschau").forEach { tab ->
                    texts(a.window.decorView).first { it.text.toString()==tab }.performClick()
                }
                assertNull(BatteryProfiles.activeId(a))
            }
            scenario.recreate()
            scenario.onActivity { a ->
                assertTrue(texts(a.window.decorView).any { it.text.toString()=="Lokale Vorschau" })
                texts(a.window.decorView).first { it.text.toString()=="Profil speichern" }.performClick()
                assertNull(BatteryProfiles.activeId(a))
                assertEquals(3,BatteryProfiles.load(a).size)
            }
        }
    }
}
