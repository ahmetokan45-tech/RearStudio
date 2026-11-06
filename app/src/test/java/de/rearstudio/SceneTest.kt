package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
class SceneTest {
 @Test fun profilesRoundTripIndependently(){val s=StudioScene.defaults();s.profile("normal").widgets[0].x=.4f;s.profile("charging").chargeStyle="ring";s.profile("charging").chargeColor=0xffaa5500.toInt();s.profile("notification").enabled=false;val r=StudioScene.parse(s.json());assertEquals(.4f,r.profile("normal").widgets[0].x,.001f);assertEquals("ring",r.profile("charging").chargeStyle);assertEquals(0xffaa5500.toInt(),r.profile("charging").chargeColor);assertFalse(r.profile("notification").enabled)}
 @Test fun emptyProfileStaysEmptyAfterSaving(){val s=StudioScene.defaults();s.profile("normal").widgets.clear();assertTrue(StudioScene.parse(s.json()).profile("normal").widgets.isEmpty())}
 @Test fun invalidValuesAreSanitizedWithoutViewportClamping(){val w=StudioWidget(kind="clock",x=Float.NaN,y=Float.POSITIVE_INFINITY,w=2f,h=-1f);w.constrain();assertTrue(w.x.isFinite());assertTrue(w.y.isFinite());assertEquals(2f,w.w,0f);assertTrue(w.h>0)}
 @Test fun offscreenAndOversizedWidgetsSurviveCopyAndRoundTrip(){val s=StudioScene.defaults();val w=s.profile("normal").widgets[0];w.x=-1.25f;w.y=2.5f;w.w=3f;w.h=1.5f;val r=s.copy().profile("normal").widgets[0];assertEquals(w.id,r.id);assertEquals(w.x,r.x,0f);assertEquals(w.y,r.y,0f);assertEquals(w.w,r.w,0f);assertEquals(w.h,r.h,0f)}

 @Test fun draftIsDeepCopy(){val a=StudioScene.defaults();val b=a.copy();b.profile("normal").widgets.clear();assertTrue(a.profile("normal").widgets.isNotEmpty())}
 @Test fun photoAndChargingOptionsSurviveRestart(){val s=StudioScene.defaults();val p=s.profile("charging");p.background="photo";p.photo="/private/example.gif";p.palette="rainbow";p.duration=0;p.chargeOpacity=44;p.animation=false;val r=StudioScene.parse(s.json()).profile("charging");assertEquals(p.photo,r.photo);assertEquals("rainbow",r.palette);assertEquals(0,r.duration);assertEquals(44,r.chargeOpacity);assertFalse(r.animation)}
}
