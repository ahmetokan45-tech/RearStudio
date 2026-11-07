package de.rearstudio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignLibraryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    @Before fun clear() { context.getSharedPreferences("studio_design_library", 0).edit().clear().commit() }
    @Test fun snapshotsPersistAndNeverAliasTheEditor() {
        val scene = StudioScene.defaults(); val library = DesignLibrary(context)
        val saved = library.save("First", scene)
        scene.profile("normal").widgets.clear()
        val reloaded = DesignLibrary(context).list().single()
        assertEquals(saved, reloaded)
        assertTrue(StudioScene.parse(reloaded.scene).profile("normal").widgets.isNotEmpty())
        val duplicate = library.duplicate(saved.id)
        assertNotEquals(saved.id, duplicate.id)
        library.rename(duplicate.id, "Second"); library.favorite(duplicate.id)
        assertEquals("Second", library.quick().single().name)
        library.delete(saved.id); assertEquals(1, library.list().size)
    }
    @Test fun backgroundOnlyPreservesWidgetsAndCalibration() {
        val current = StudioScene.defaults().apply { geometry = DisplayGeometry(lenses=listOf(LensCircle(100f,150f,50f))) }
        val candidate = StudioScene.defaults().apply { profile("normal").widgets.clear(); profile("normal").color=123 }
        val before = current.json()
        val result = DesignLibrary.compose(current, candidate, true)
        assertEquals(before, current.json())
        assertEquals(123, result.profile("normal").color)
        assertEquals(current.profile("normal").widgets.size, result.profile("normal").widgets.size)
        assertEquals(current.geometry, result.geometry)
        assertEquals(current.geometry, DesignLibrary.compose(current,candidate,false).geometry)
    }
    @Test fun blankNamesRejectedAndFavoritesLimited() {
        val library=DesignLibrary(context)
        assertThrows(IllegalArgumentException::class.java){library.save("  ",StudioScene.defaults())}
        repeat(12){library.favorite(library.save("Design $it",StudioScene.defaults()).id)}
        assertEquals(10,library.quick().size)
    }
    @Test fun undoRedoRestoresIndependentSnapshotsAndBranches() {
        val history=SceneHistory();val original=StudioScene.defaults();history.record(original)
        val edited=original.copy().apply{profile("normal").widgets.clear()}
        val restored=history.undo(edited)!!;assertEquals(original.json(),restored.json())
        assertEquals(edited.json(),history.redo(restored)!!.json())
        history.undo(edited);history.record(original);assertNull(history.redo(original))
    }
    @Test fun extendedWidgetPropertiesAndSmartProfilesRoundTrip() {
        val scene=StudioScene.defaults();val w=scene.profile("normal").widgets.first()
        w.locked=true;w.rotation=47f;w.options.put("text","Türkçe العربية")
        scene.profile("smart_music").widgets.add(StudioWidget(kind="music"))
        val copy=scene.copy();val found=copy.profile("normal").widgets.first()
        assertTrue(found.locked);assertEquals(47f,found.rotation,0f)
        assertEquals("Türkçe العربية",found.options.getString("text"))
        found.options.put("text","Changed");assertNotEquals(w.options.toString(),found.options.toString())
        assertEquals("music",copy.profile("smart_music").widgets.single().kind)
    }
}
