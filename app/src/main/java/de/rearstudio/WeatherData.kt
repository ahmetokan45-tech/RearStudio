package de.rearstudio

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/** Explicit location opt-in, bounded HTTPS response, offline cache with its observation time. */
object WeatherData {
 private val worker=Executors.newSingleThreadExecutor()
 private var busy=false
 private var attemptedAt=0L
 data class Reading(val temperature:Double,val humidity:Int,val wind:Double,val code:Int,val observed:String)
 fun parse(raw:String):Reading {
  val c=JSONObject(raw).getJSONObject("current")
  val t=c.getDouble("temperature_2m");val h=c.getInt("relative_humidity_2m");val w=c.getDouble("wind_speed_10m");val code=c.getInt("weather_code")
  require(t.isFinite()&&t in -100.0..70.0&&h in 0..100&&w.isFinite()&&w in 0.0..500.0)
  return Reading(t,h,w,code,c.getString("time"))
 }
 fun read(c:Context):Reading?=c.getSharedPreferences("studio_weather",0).getString("response",null)?.let{runCatching{parse(it)}.getOrNull()}
 fun stale(c:Context)=System.currentTimeMillis()-c.getSharedPreferences("studio_weather",0).getLong("fetched",0)>60*60*1000
 fun configure(c:Context,latitude:Double,longitude:Double){
  require(latitude.isFinite()&&longitude.isFinite()&&latitude in -90.0..90.0&&longitude in -180.0..180.0)
  c.getSharedPreferences("studio_weather",0).edit().putString("latitude",latitude.toString()).putString("longitude",longitude.toString()).remove("response").putLong("fetched",0).apply()
  attemptedAt=0;refresh(c,true)
 }
 fun disable(c:Context){c.getSharedPreferences("studio_weather",0).edit().clear().apply()}
 @Synchronized fun refresh(c:Context,force:Boolean=false){
  val app=c.applicationContext;val prefs=app.getSharedPreferences("studio_weather",0)
  val lat=prefs.getString("latitude",null)?:return;val lon=prefs.getString("longitude",null)?:return
  val now=System.currentTimeMillis()
  if(busy||(!force&&(now-attemptedAt<15*60*1000||now-prefs.getLong("fetched",0)<30*60*1000)))return
  busy=true;attemptedAt=now
  worker.execute{
   var connection:HttpURLConnection?=null
   try {
    val query="latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code&timezone=auto"
    connection=URL("https://api.open-meteo.com/v1/forecast?$query").openConnection() as HttpURLConnection
    connection.connectTimeout=10000;connection.readTimeout=10000;connection.instanceFollowRedirects=false
    require(connection.responseCode==200)
    val bytes=connection.inputStream.use{input->val output=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=input.read(buffer);if(n<0)break;require(output.size()+n<=512*1024);output.write(buffer,0,n)};output.toByteArray()}
    val raw=bytes.toString(Charsets.UTF_8);parse(raw)
    // Ignore a response for a location that was removed or changed while fetching.
    if(prefs.getString("latitude",null)==lat&&prefs.getString("longitude",null)==lon)prefs.edit().putString("response",raw).putLong("fetched",System.currentTimeMillis()).apply()
   } catch(_:Exception) { /* Keep the last real observation; renderer labels its age. */ }
   finally{connection?.disconnect();synchronized(this){busy=false}}
  }
 }
 fun condition(code:Int)=when(code){0->"weather_clear";1,2,3->"weather_cloudy";45,48->"weather_fog";51,53,55,56,57,61,63,65,66,67,80,81,82->"weather_rain";71,73,75,77,85,86->"weather_snow";95,96,99->"weather_storm";else->"weather_unknown"}
}
