package de.rearstudio
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
class SmartViewsTest {
 @Test fun defaultsOpenAllViewsAndCloseWithoutSceneMutation(){
  val scene=StudioScene.defaults();val before=scene.json();val nav=SmartViews()
  for((direction,view) in SmartViews.DEFAULTS){nav.swipe(direction,scene.navigation,0);assertEquals(view,nav.opened);nav.swipe(SwipeDirection.DOWN,scene.navigation,1);assertNull(nav.opened)}
  assertEquals(before,scene.json())
 }
 @Test fun inactivityRestartsAfterInteractionAndCanBeDisabled(){
  val n=SmartViews();n.open("smart_music",0);n.interact(20_000);assertFalse(n.expire(49_999,30));assertTrue(n.expire(50_000,30))
  n.open("smart_weather",0);assertFalse(n.expire(500_000,0));assertNotNull(n.opened)
 }
 @Test fun directionRequiresDistanceAndUsesDominantAxis(){
  assertNull(SmartViews.direction(10f,10f,48f));assertEquals(SwipeDirection.LEFT,SmartViews.direction(-90f,20f,48f))
  assertNull(SmartViews.direction(Float.NaN,10f,48f))
 }
 @Test fun customMappingAndCloseGestureWorkIndependently(){
  val n=SmartViews();val settings=JSONObject().put("UP","smart_music").put("close","LEFT")
  n.swipe(SwipeDirection.UP,settings,0);n.swipe(SwipeDirection.DOWN,settings,1);assertEquals("smart_music",n.opened)
  n.swipe(SwipeDirection.LEFT,settings,2);assertNull(n.opened)
 }
}
