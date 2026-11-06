package de.rearstudio

import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DisplayGeometryTest {
    @Test fun legacyScenesHaveNoInventedCameraMask() {
        val scene=StudioScene.defaults()
        assertNull(StudioScene.parse(scene.json()).geometry)
        assertTrue(DisplayGeometry().lenses.isEmpty())
    }
    @Test fun manualCalibrationRoundTripsWithSceneWithoutMovingWidgets() {
        val scene=StudioScene.defaults()
        scene.profile("normal").widgets[0].x=-.5f
        scene.geometry=DisplayGeometry(corners=listOf(90f,100f,101f,98f),lenses=listOf(LensCircle(120f,150f,60f),LensCircle(120f,440f,62f)),source="geometry_custom")
        val copy=scene.copy()
        assertEquals(scene.geometry,copy.geometry)
        assertEquals(-.5f,copy.profile("normal").widgets[0].x,0f)
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        StudioScene.save(context,scene)
        assertEquals(scene.geometry,StudioScene.load(context).geometry)
    }
    @Test fun edgeAndClipUseSameContourWithStrokeInset() {
        val geometry=DisplayGeometry()
        val outer=RectF();geometry.outline(976f,596f).computeBounds(outer,true)
        val edge=RectF();geometry.outline(976f,596f,8f).computeBounds(edge,true)
        assertEquals(RectF(0f,0f,976f,596f),outer)
        assertEquals(RectF(8f,8f,968f,588f),edge)
        val full=PathMeasure(geometry.outline(976f,596f),true).length
        val half=PathMeasure(geometry.outline(488f,298f),true).length
        assertEquals(full/2f,half,1f)
    }
    @Test fun invalidCalibrationDoesNotDiscardExistingScene() {
        val scene=StudioScene.defaults();scene.profile("normal").widgets[0].x=-.75f
        val json=org.json.JSONObject(scene.json()).put("geometry",DisplayGeometry().json().put("width",-1))
        val read=StudioScene.parse(json.toString())
        assertNull(read.geometry)
        assertEquals(-.75f,read.profile("normal").widgets[0].x,0f)
    }
    @Test fun cancellingCalibrationDraftLeavesSavedSceneUnchanged() {
        val original=StudioScene.defaults()
        val draft=original.copy();draft.geometry=DisplayGeometry(source="geometry_custom")
        assertNull(original.geometry)
        assertNotNull(draft.geometry)
    }
    @Test fun degenerateViewportHasEmptyPath() {
        assertTrue(DisplayGeometry().outline(0f,596f).isEmpty)
    }
}
