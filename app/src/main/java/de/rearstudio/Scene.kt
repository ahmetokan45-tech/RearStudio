package de.rearstudio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StudioWidget(val id: String = UUID.randomUUID().toString(), var kind: String, var x: Float = .35f, var y: Float = .1f, var w: Float = .58f, var h: Float = .32f, var color: Int = 0xffefeff3.toInt(), var opacity: Int = 100, var style: String = "digital", var enabled: Boolean = true, var locked: Boolean = false, var rotation: Float = 0f, var options: JSONObject = JSONObject()) {
    /** Validate values only: the viewport and camera guides never constrain placement. */
    fun constrain() { w=w.finite(.58f).coerceAtLeast(.001f);h=h.finite(.32f).coerceAtLeast(.001f);x=x.finite(.35f);y=y.finite(.1f);opacity=opacity.coerceIn(0,100);rotation=rotation.finite(0f)%360f }
    fun json()=JSONObject().put("id",id).put("kind",kind).put("x",x).put("y",y).put("w",w).put("h",h).put("color",color).put("opacity",opacity).put("style",style).put("enabled",enabled).put("locked",locked).put("rotation",rotation).put("options",options)
    companion object { fun read(o:JSONObject)=StudioWidget(o.optString("id",UUID.randomUUID().toString()),o.getString("kind"),o.optDouble("x",.35).toFloat(),o.optDouble("y",.1).toFloat(),o.optDouble("w",.58).toFloat(),o.optDouble("h",.32).toFloat(),o.optInt("color",0xffefeff3.toInt()),o.optInt("opacity",100),o.optString("style","digital"),o.optBoolean("enabled",true),o.optBoolean("locked",false),o.optDouble("rotation",0.0).toFloat(),o.optJSONObject("options")?:JSONObject()).apply{constrain()} }
}
private fun Float.finite(fallback:Float)=if(isFinite())this else fallback

data class StudioProfile(var enabled:Boolean=true,var background:String="gradient",var photo:String="",var color:Int=0xff252335.toInt(),var color2:Int=0xff533d43.toInt(),var dim:Int=20,var chargeEnabled:Boolean=true,var chargeStyle:String="edge",var chargeColor:Int=0xffe6bd77.toInt(),var chargeColor2:Int=0xffe06d87.toInt(),var palette:String="gradient",var thickness:Int=8,var chargeOpacity:Int=95,var glow:Boolean=true,var percentage:Boolean=true,var animation:Boolean=true,var duration:Int=10,var reuseNormal:Boolean=false,var popup:Boolean=true,val widgets:MutableList<StudioWidget> = mutableListOf(), var cameraLight:LightAnimation = LightAnimation(), var imageOptions:JSONObject = JSONObject()) {
    fun json()=JSONObject().put("enabled",enabled).put("background",background).put("photo",photo).put("color",color).put("color2",color2).put("dim",dim).put("chargeEnabled",chargeEnabled).put("chargeStyle",chargeStyle).put("chargeColor",chargeColor).put("chargeColor2",chargeColor2).put("palette",palette).put("thickness",thickness).put("chargeOpacity",chargeOpacity).put("glow",glow).put("percentage",percentage).put("animation",animation).put("duration",duration).put("reuseNormal",reuseNormal).put("popup",popup).put("widgets",JSONArray(widgets.map{it.json()})).put("cameraLight",cameraLight.json()).put("imageOptions",imageOptions)
    companion object { fun read(o:JSONObject):StudioProfile=StudioProfile().apply {
        enabled=o.optBoolean("enabled",true);background=o.optString("background","gradient");photo=o.optString("photo");color=o.optInt("color",color);color2=o.optInt("color2",color2);dim=o.optInt("dim",20).coerceIn(0,90)
        chargeEnabled=o.optBoolean("chargeEnabled",true);chargeStyle=o.optString("chargeStyle","edge");chargeColor=o.optInt("chargeColor",chargeColor);chargeColor2=o.optInt("chargeColor2",chargeColor2);palette=o.optString("palette","gradient");thickness=o.optInt("thickness",8).coerceIn(3,20);chargeOpacity=o.optInt("chargeOpacity",95).coerceIn(0,100);glow=o.optBoolean("glow",true);percentage=o.optBoolean("percentage",true);animation=o.optBoolean("animation",true);duration=o.optInt("duration",10).coerceIn(0,120);reuseNormal=o.optBoolean("reuseNormal",false);popup=o.optBoolean("popup",true)
        imageOptions=o.optJSONObject("imageOptions")?:JSONObject();cameraLight=o.optJSONObject("cameraLight")?.let{LightAnimation.read(it)}?:LightAnimation();val a=o.optJSONArray("widgets")?:JSONArray();for(i in 0 until minOf(128,a.length())){try{widgets.add(StudioWidget.read(a.getJSONObject(i)))}catch(_:Exception){}}
    } }
}

data class StudioScene(var accent:Int=0xffe6bd77.toInt(),val profiles:MutableMap<String,StudioProfile> = mutableMapOf(),var geometry:DisplayGeometry?=null,var navigation:JSONObject=JSONObject()) {
    fun profile(name:String)=profiles.getOrPut(name){
        if(name in SmartViews.VIEWS) StudioProfile(widgets=mutableListOf(StudioWidget(kind=when(name){"smart_music"->"music";"smart_notifications"->"notification";"smart_activity"->"activity";"smart_weather"->"weather";else->"unavailable"},x=.35f,y=.18f,w=.59f,h=.65f))) else StudioProfile()
    }
    fun json()=JSONObject().put("version",1).put("geometry",geometry?.json()).put("navigation",navigation).put("accent",accent).put("profiles",JSONObject().apply{profiles.forEach{(k,v)->put(k,v.json())}}).toString()
    fun copy()=parse(json())
    companion object {
        fun defaults()=StudioScene().apply {
            profiles["normal"]=StudioProfile(widgets=mutableListOf(StudioWidget(kind="clock"),StudioWidget(kind="football",y=.55f,h=.30f,color=accent)))
            profiles["charging"]=StudioProfile(widgets=mutableListOf(StudioWidget(kind="clock"),StudioWidget(kind="battery",y=.6f,w=.4f,h=.2f,color=0xffa4c6bc.toInt())))
            profiles["notification"]=StudioProfile(widgets=mutableListOf(StudioWidget(kind="clock"),StudioWidget(kind="notification",x=.32f,y=.52f,w=.64f,h=.32f,color=accent)))
        }
        fun parse(raw:String):StudioScene {val o=JSONObject(raw);require(o.getInt("version")==1);val p=o.getJSONObject("profiles");return StudioScene(o.optInt("accent",0xffe6bd77.toInt())).apply{navigation=o.optJSONObject("navigation")?:JSONObject();geometry=o.optJSONObject("geometry")?.let{runCatching{DisplayGeometry.read(it)}.getOrNull()};for(name in p.keys())profiles[name]=StudioProfile.read(p.getJSONObject(name));for(name in listOf("normal","charging","notification"))profile(name)}}
        fun load(context:Context,preview:Boolean=false):StudioScene {val p=context.getSharedPreferences("studio",0);val draft=if(preview&&System.currentTimeMillis()-p.getLong("preview_time",0)<15_000)p.getString("preview",null) else null;return try{parse(draft?:p.getString("scene",null)?:return defaults())}catch(_:Exception){defaults()}}
        fun save(context:Context,scene:StudioScene){context.getSharedPreferences("studio",0).edit().putString("scene",scene.json()).putBoolean("scene_enabled",true).apply()}
    }
}
