package de.rearstudio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.time.LocalDate

/** Counts only observed increments. Never invents a complete pre-install or reboot history. */
data class StepDay(val date:String,val steps:Long,val lastCounter:Long) {
 fun observe(day:String,counter:Long):StepDay {
  require(counter>=0)
  if(day!=date)return StepDay(day,0,counter)
  val increment=if(counter>=lastCounter)counter-lastCounter else counter
  return copy(steps=steps+increment,lastCounter=counter)
 }
}
object ActivityData:SensorEventListener {
 private var context:Context?=null
 private var manager:SensorManager?=null
 private var listening=false
 fun start(c:Context):Boolean {
  if(c.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED){stop();return false}
  if(listening)return true
  context=c.applicationContext;manager=c.getSystemService(SensorManager::class.java)
  val sensor=manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?:return false
  listening=runCatching{manager?.registerListener(this,sensor,SensorManager.SENSOR_DELAY_NORMAL)==true}.getOrDefault(false)
  return listening
 }
 fun stop(){manager?.unregisterListener(this);listening=false}
 fun available(c:Context)=c.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)==PackageManager.PERMISSION_GRANTED && c.getSystemService(SensorManager::class.java).getDefaultSensor(Sensor.TYPE_STEP_COUNTER)!=null
 fun today(c:Context):Long? {val p=c.getSharedPreferences("activity_steps",0);val key="day:${LocalDate.now()}";return if(p.contains(key))p.getLong(key,0) else null}
 fun week(c:Context):List<Long?> {val p=c.getSharedPreferences("activity_steps",0);return (6 downTo 0).map{val key="day:${LocalDate.now().minusDays(it.toLong())}";if(p.contains(key))p.getLong(key,0)else null}}
 override fun onSensorChanged(event:SensorEvent){
  if(event.sensor.type!=Sensor.TYPE_STEP_COUNTER || event.values.isEmpty() || !event.values[0].isFinite())return
  val c=context?:return;val p=c.getSharedPreferences("activity_steps",0);val day=LocalDate.now().toString();val counter=event.values[0].toLong().coerceAtLeast(0)
  val old=StepDay(p.getString("date",day)?:day,p.getLong("day:$day",0),p.getLong("counter",counter))
  val next=old.observe(day,counter)
  val edit=p.edit().putString("date",day).putLong("counter",counter).putLong("day:$day",next.steps)
  p.all.keys.filter{it.startsWith("day:") && it.substringAfter(":") < LocalDate.now().minusDays(30).toString()}.forEach{edit.remove(it)}
  edit.apply()
 }
 override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int){}
}
