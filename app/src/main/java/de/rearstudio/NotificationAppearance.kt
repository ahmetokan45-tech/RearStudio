package de.rearstudio
import android.content.Context
import org.json.JSONObject
object NotificationAppearance {
 val styles=listOf("compact","card","liquid","gradient","minimal")
 fun apply(context:Context,widget:StudioWidget,packageName:String?):StudioWidget {
  if(packageName==null)return widget
  val raw=context.getSharedPreferences("notification_appearance",0).getString(packageName,null)?:return widget
  return runCatching{val o=JSONObject(raw);widget.copy(style=o.optString("style",widget.style).takeIf{it in styles}?:widget.style,color=o.optInt("color",widget.color))}.getOrDefault(widget)
 }
 fun save(context:Context,packageName:String,style:String,color:Int){require(style in styles);context.getSharedPreferences("notification_appearance",0).edit().putString(packageName,JSONObject().put("style",style).put("color",color).toString()).apply()}
 fun clear(context:Context,packageName:String){context.getSharedPreferences("notification_appearance",0).edit().remove(packageName).apply()}
}
