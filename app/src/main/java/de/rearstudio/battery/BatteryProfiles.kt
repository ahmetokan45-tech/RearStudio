package de.rearstudio.battery

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BatteryProfile(
    val id: String,
    var name: String,
    var style: ChargingStyle = ChargingStyle.EDGE_GLOW,
    var color: Int = 0xffb5f36d.toInt(),
    var connected: Boolean = true,
    var full: Boolean = true,
    var wake: Boolean = true,
    var overXiaomi: Boolean = true,
)

/** Profile storage is independent from the active renderer settings; only activation applies it. */
object BatteryProfiles {
    private fun prefs(c: Context) = c.getSharedPreferences("battery_studio_v1", 0)
    fun load(c: Context): MutableList<BatteryProfile> = runCatching {
        val a = JSONArray(prefs(c).getString("profiles", "[]"))
        MutableList(a.length()) { i -> val o = a.getJSONObject(i)
            BatteryProfile(o.getString("id"), o.getString("name"),
                ChargingStyle.entries.firstOrNull { it.name == o.optString("style") } ?: ChargingStyle.RING,
                o.optInt("color", 0xffb5f36d.toInt()), o.optBoolean("connected", true),
                o.optBoolean("full", true), o.optBoolean("wake", true), o.optBoolean("over", true))
        }
    }.getOrDefault(mutableListOf()).ifEmpty {
        mutableListOf(BatteryProfile("edge", "Edge Charging", BatterySettings.style(c), BatterySettings.color(c) ?: 0xffb5f36d.toInt(), wake = BatterySettings.lightUp(c), overXiaomi = BatterySettings.overXiaomi(c)),
            BatteryProfile("minimal", "Minimal", ChargingStyle.MINIMAL, 0xffaabfff.toInt()),
            BatteryProfile("ring", "Signature Ring", ChargingStyle.RING, 0xffffbd78.toInt()))
    }
    fun save(c: Context, profiles: List<BatteryProfile>) {
        val a = JSONArray()
        profiles.forEach { p -> a.put(JSONObject().put("id", p.id).put("name", p.name)
            .put("style", p.style.name).put("color", p.color).put("connected", p.connected)
            .put("full", p.full).put("wake", p.wake).put("over", p.overXiaomi)) }
        prefs(c).edit().putString("profiles", a.toString()).apply()
    }
    fun activeId(c: Context): String? = prefs(c).getString("active", null)
    fun activate(c: Context, p: BatteryProfile) {
        prefs(c).edit().putString("active", p.id).apply()
        BatterySettings.setStyle(c, p.style); BatterySettings.setColor(c, p.color)
        BatterySettings.setLightUp(c, p.wake); BatterySettings.setOverXiaomi(c, p.overXiaomi)
        BatterySettings.setEvents(c, p.connected, p.full)
    }
}

/** Full fires once per charging session, including a later 100% update, never at initial registration. */
class FullChargeGate {
    private var wasPlugged = false
    private var fired = false
    fun update(plugged: Boolean, full: Boolean): Boolean {
        val fire = plugged && wasPlugged && full && !fired
        if (!plugged) fired = false else if (full) fired = true
        wasPlugged = plugged
        return fire
    }
}
