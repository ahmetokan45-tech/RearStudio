package de.rearstudio

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class StudioWidget(val id: String = UUID.randomUUID().toString(), var kind: String, var x: Float = .35f, var y: Float = .1f, var w: Float = .58f, var h: Float = .32f, var color: Int = 0xffefeff3.toInt(), var opacity: Int = 100, var style: String = "digital", var enabled: Boolean = true, var condition:String="always", var hideBelow:Boolean=false, var locked:Boolean=false, var action:String="none", var target:String="rear") {
    fun constrain() { w=w.finite(.58f).coerceIn(.15f,1f);h=h.finite(.32f).coerceIn(.12f,1f);x=x.finite(.35f).coerceIn(0f,1-w);y=y.finite(.1f).coerceIn(0f,1-h);opacity=opacity.coerceIn(0,100) }
    fun json()=JSONObject().put("id",id).put("kind",kind).put("x",x).put("y",y).put("w",w).put("h",h).put("color",color).put("opacity",opacity).put("style",style).put("enabled",enabled).put("condition",condition).put("hideBelow",hideBelow).put("locked",locked).put("action",action).put("target",target)
    companion object { fun read(o:JSONObject)=StudioWidget(o.optString("id",UUID.randomUUID().toString()),o.getString("kind"),o.optDouble("x",.35).toFloat(),o.optDouble("y",.1).toFloat(),o.optDouble("w",.58).toFloat(),o.optDouble("h",.32).toFloat(),o.optInt("color",0xffefeff3.toInt()),o.optInt("opacity",100),o.optString("style","digital"),o.optBoolean("enabled",true),o.optString("condition","always"),o.optBoolean("hideBelow",false),o.optBoolean("locked",false),o.optString("action","none"),o.optString("target","rear")).apply{constrain()} }
}
private fun Float.finite(fallback:Float)=if(isFinite())this else fallback

data class StudioProfile(var enabled:Boolean=true,var background:String="gradient",var photo:String="",var color:Int=0xff252335.toInt(),var color2:Int=0xff533d43.toInt(),var dim:Int=20,var chargeEnabled:Boolean=true,var chargeStyle:String="edge",var chargeColor:Int=0xffe6bd77.toInt(),var chargeColor2:Int=0xffe06d87.toInt(),var palette:String="gradient",var thickness:Int=8,var chargeOpacity:Int=95,var glow:Boolean=true,var percentage:Boolean=true,var animation:Boolean=true,var duration:Int=10,var reuseNormal:Boolean=false,var popup:Boolean=true,val widgets:MutableList<StudioWidget> = mutableListOf()) {
    fun json()=JSONObject().put("enabled",enabled).put("background",background).put("photo",photo).put("color",color).put("color2",color2).put("dim",dim).put("chargeEnabled",chargeEnabled).put("chargeStyle",chargeStyle).put("chargeColor",chargeColor).put("chargeColor2",chargeColor2).put("palette",palette).put("thickness",thickness).put("chargeOpacity",chargeOpacity).put("glow",glow).put("percentage",percentage).put("animation",animation).put("duration",duration).put("reuseNormal",reuseNormal).put("popup",popup).put("widgets",JSONArray(widgets.map{it.json()}))
    companion object { fun read(o:JSONObject):StudioProfile=StudioProfile().apply {
        enabled=o.optBoolean("enabled",true);background=o.optString("background","gradient");photo=o.optString("photo");color=o.optInt("color",color);color2=o.optInt("color2",color2);dim=o.optInt("dim",20).coerceIn(0,90)
        chargeEnabled=o.optBoolean("chargeEnabled",true);chargeStyle=o.optString("chargeStyle","edge");chargeColor=o.optInt("chargeColor",chargeColor);chargeColor2=o.optInt("chargeColor2",chargeColor2);palette=o.optString("palette","gradient");thickness=o.optInt("thickness",8).coerceIn(3,20);chargeOpacity=o.optInt("chargeOpacity",95).coerceIn(0,100);glow=o.optBoolean("glow",true);percentage=o.optBoolean("percentage",true);animation=o.optBoolean("animation",true);duration=o.optInt("duration",10).coerceIn(0,120);reuseNormal=o.optBoolean("reuseNormal",false);popup=o.optBoolean("popup",true)
        val a=o.optJSONArray("widgets")?:JSONArray();for(i in 0 until minOf(18,a.length())){try{widgets.add(StudioWidget.read(a.getJSONObject(i)))}catch(_:Exception){}}
    } }
}

data class StudioScene(var accent:Int=0xffe6bd77.toInt(),val profiles:MutableMap<String,StudioProfile> = mutableMapOf(), var unified:Boolean=false, var djEnabled:Boolean=false, val favorites:MutableList<Int> = mutableListOf(0xffe6bd77.toInt(),0xffacd4c1.toInt(),0xffc5b5e6.toInt(),0xffedb0a6.toInt(),0xffefeff3.toInt()), val gestures:MutableMap<String,String> = mutableMapOf("left" to "music","down" to "notifications","right" to "camera","up" to "sport")) {
    fun profile(name:String)=profiles.getOrPut(name){StudioProfile()}
    fun json()=JSONObject().put("version",2).put("unified",unified).put("djEnabled",djEnabled).put("favorites",JSONArray(favorites)).put("gestures",JSONObject(gestures.toMap())).put("accent",accent).put("profiles",JSONObject().apply{profiles.forEach{(k,v)->put(k,v.json())}}).toString()
    fun copy()=parse(json())
    fun toUnified():StudioScene {
        if(unified)return this
        val base=profile("normal").widgets
        for((name,condition) in listOf("charging" to "charging","notification" to "notification"))if(profile(name).enabled)for(w in profile(name).widgets){if(w.kind!="clock"&&base.size<18&&base.none{it.id==w.id}){val added=StudioWidget.read(w.json());added.condition=condition;added.hideBelow=name=="notification";if(added.kind=="battery")added.style=profile("charging").chargeStyle;base.add(added)}}
        base.filter{it.kind=="music"}.forEach{it.condition="music"}
        if(base.size<18&&base.none{it.kind=="music"})base.add(StudioWidget(kind="music",x=.34f,y=.55f,w=.6f,h=.32f,style="compact",condition="music",hideBelow=true))
        unified=true;return this
    }
    companion object {
        fun defaults()=StudioScene().apply {
            profiles["normal"]=StudioProfile(widgets=mutableListOf(StudioWidget(kind="clock"),StudioWidget(kind="football",y=.55f,h=.30f,color=accent)))
            profiles["charging"]=StudioProfile(widgets=mutableListOf(StudioWidget(kind="clock"),StudioWidget(kind="battery",y=.6f,w=.4f,h=.2f,color=0xffa4c6bc.toInt())))
            profiles["notification"]=StudioProfile(widgets=mutableListOf(StudioWidget(kind="clock"),StudioWidget(kind="notification",x=.32f,y=.52f,w=.64f,h=.32f,color=accent)))
        }
        fun parse(raw:String):StudioScene {val o=JSONObject(raw);require(o.getInt("version") in 1..2);val p=o.getJSONObject("profiles");return StudioScene(o.optInt("accent",0xffe6bd77.toInt())).apply{for(name in listOf("normal","charging","notification"))profiles[name]=StudioProfile.read(p.getJSONObject(name));unified=o.optBoolean("unified",false);djEnabled=o.optBoolean("djEnabled",false);o.optJSONArray("favorites")?.let{a->if(a.length()==5){favorites.clear();for(i in 0..4)favorites.add(a.getInt(i))}};o.optJSONObject("gestures")?.let{g->for(k in gestures.keys.toList())gestures[k]=g.optString(k,gestures[k]?:"none")}}}
        fun load(context:Context,preview:Boolean=false):StudioScene {val p=context.getSharedPreferences("studio",0);val draft=if(preview&&System.currentTimeMillis()-p.getLong("preview_time",0)<15_000)p.getString("preview",null) else null;return try{parse(draft?:p.getString("scene",null)?:return defaults().toUnified()).let{if(it.unified)it else it.toUnified()}}catch(_:Exception){defaults().toUnified()}}
        fun save(context:Context,scene:StudioScene){context.getSharedPreferences("studio",0).edit().putString("scene",scene.json()).putBoolean("scene_enabled",true).apply()}
    }
}
