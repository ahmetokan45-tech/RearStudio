package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONArray
import org.json.JSONObject
class GradientSpecTest {
 @Test fun stopsSortAndSurviveRoundTrip(){val w=StudioWidget(kind="gradient");GradientSpec.write(w,listOf(GradientStop(1f,3),GradientStop(0f,1),GradientStop(.4f,2)));val copy=StudioWidget.read(JSONObject(w.json().toString()));assertEquals(listOf(0f,.4f,1f),GradientSpec.read(copy).stops.map{it.position})}
 @Test fun duplicateStopsRejected(){assertThrows(IllegalArgumentException::class.java){GradientSpec.write(StudioWidget(kind="gradient"),listOf(GradientStop(0f,1),GradientStop(0f,2)))}}
 @Test fun corruptStopsFallbackAndPeriodBounded(){val w=StudioWidget(kind="gradient");w.options.put("stops",JSONArray().put(JSONObject().put("position",.2))).put("period",0);val g=GradientSpec.read(w);assertEquals(2,g.stops.size);assertEquals(500L,g.periodMs)}
 @Test fun reducedMotionAndPeriodicRotation(){val g=GradientSpec(listOf(GradientStop(0f,0),GradientStop(1f,1)),45f,1000,true);assertEquals(225f,g.angleAt(500,false),.01f);assertEquals(45f,g.angleAt(1000,false),.01f);assertEquals(45f,g.angleAt(500,true),.01f)}
 @Test fun eightStopLimit(){val w=StudioWidget(kind="gradient");assertThrows(IllegalArgumentException::class.java){GradientSpec.write(w,(0..8).map{GradientStop(it/8f,it)})}}
}
