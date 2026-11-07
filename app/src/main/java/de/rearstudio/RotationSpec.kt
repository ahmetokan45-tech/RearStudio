package de.rearstudio
import org.json.JSONObject
object RotationSpec {
 val kinds=listOf("music","weather","activity","battery","date")
 fun slides(widget:StudioWidget):List<Pair<String,Int>> {
  val a=widget.options.optJSONArray("slides")
  val result=(0 until minOf(a?.length()?:0,8)).mapNotNull{i->val o=a?.optJSONObject(i)?:return@mapNotNull null;val kind=o.optString("kind");if(kind !in kinds)null else kind to o.optInt("seconds",10).coerceIn(2,120)}
  return result.ifEmpty{listOf("music" to 10,"weather" to 10,"activity" to 10)}
 }
 fun index(widget:StudioWidget,elapsed:Long,manual:Int?):Int {
  val slides=slides(widget)
  if(manual!=null)return Math.floorMod(manual,slides.size)
  if(!widget.options.optBoolean("autoRotate",true))return 0
  var position=(elapsed.coerceAtLeast(0)/1000)%slides.sumOf{it.second}
  slides.forEachIndexed{i,s->if(position<s.second)return i;position-=s.second}
  return 0
 }
 fun shown(widget:StudioWidget,elapsed:Long,manual:Int?):StudioWidget {
  if(widget.kind!="rotation_widget")return widget
  return widget.copy(kind=slides(widget)[index(widget,elapsed,manual)].first,options=JSONObject(widget.options.toString()))
 }
}
