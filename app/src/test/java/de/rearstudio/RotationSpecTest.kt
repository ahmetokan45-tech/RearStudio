package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
class RotationSpecTest {
 @Test fun timingWrapAndManualOverride(){val w=StudioWidget(kind="rotation_widget");assertEquals(0,RotationSpec.index(w,0,null));assertEquals(1,RotationSpec.index(w,10000,null));assertEquals(0,RotationSpec.index(w,30000,null));assertEquals(2,RotationSpec.index(w,0,-1));w.options.put("autoRotate",false);assertEquals(0,RotationSpec.index(w,12000,null))}
 @Test fun shownChildPreservesParentAndOriginal(){val w=StudioWidget(kind="rotation_widget",x=.4f,rotation=30f);val child=RotationSpec.shown(w,15000,null);assertEquals("weather",child.kind);assertEquals(w.id,child.id);assertEquals(w.x,child.x);assertEquals("rotation_widget",w.kind)}
}
