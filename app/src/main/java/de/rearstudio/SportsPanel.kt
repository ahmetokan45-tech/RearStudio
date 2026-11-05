package de.rearstudio

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

/** Scores are rendered natively; this screen contains no browser launches or WebViews. */
class SportsPanel(private val activity: Activity, parent: LinearLayout) {
    private val prefs = activity.getSharedPreferences("sport", Activity.MODE_PRIVATE)
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var closed = false
    private val busy = mutableSetOf<String>()
    private val attempted = mutableMapOf<String, Long>()
    private val buttons = mutableMapOf<String, MutableList<Button>>()
    private var teamId = prefs.getString("results_team_id", "133804") ?: "133804"
    private lateinit var team: TextView
    private lateinit var footballResult: TextView
    private fun dp(n: Int) = (n * activity.resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 14f) = TextView(activity).apply {
        text = value; textSize = size; setTextColor(Color.WHITE); setPadding(0, dp(4), 0, dp(8))
        setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun card(parent: LinearLayout, color: Int): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(20).toFloat() }
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
    }
    private fun button(parent: LinearLayout, value: String, group: String, action: () -> Unit) = Button(activity).apply {
        text = value; isAllCaps = false; textSize = 14f; minHeight = dp(48)
        setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(0xff354257.toInt())
        parent.addView(this, LinearLayout.LayoutParams(-1, -2)); setOnClickListener { action() }
        buttons.getOrPut(group) { mutableListOf() }.add(this)
    }
    private fun t(key:String)=StudioUi.text(activity,key)
    private fun compact(key:String,raw:String):String {
        val lines=raw.lines().filter{it.isNotBlank()}
        return if(key=="football"&&lines.size>=3&&lines[1].matches(Regex("[0-9]+ : [0-9]+"))) "${lines[0].take(3).uppercase()}    ${lines[1]}    ${lines[2].take(3).uppercase()}\n${lines.getOrNull(3)?:""}"
        else if(key=="f1-results") lines.firstOrNull().orEmpty()+"\n"+lines.filter{it.matches(Regex("^[0-9A-Z]+\\. .+"))}.take(3).joinToString("\n")
        else raw
    }
    private fun detail(key:String){AlertDialog.Builder(activity).setTitle(t("details")).setMessage(prefs.getString("latest_$key",null)?:t("load_results")).setPositiveButton(t("close"),null).show()}
    init {
        val football=card(parent,0xff20222b.toInt())
        team=text(prefs.getString("results_team_name","Galatasaray")?:"Galatasaray",18f);football.addView(team)
        footballResult=text(prefs.getString("latest_football",null)?.let{compact("football",it)}?:t("load_results"),24f).apply{typeface=android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL);maxLines=4;ellipsize=android.text.TextUtils.TruncateAt.END};football.addView(footballResult)
        football.addView(text(t("published"),11f))
        button(football,t("refresh"),"football"){fetchFootball()}
        button(football,t("select_club"),"football"){editTeam()}
        button(football,t("details"),"football"){detail("football")}
        val f1=card(parent,0xff20222b.toInt());f1.addView(text(t("f1"),18f))
        val result=text(prefs.getString("latest_f1-results",null)?.let{compact("f1-results",it)}?:t("load_results"),20f).apply{maxLines=4;ellipsize=android.text.TextUtils.TruncateAt.END};f1.addView(result);f1.addView(text(t("published"),11f))
        button(f1,t("refresh"),"f1-results"){request("f1-results",result,t("loading")){SportResults.f1(get("https://api.jolpi.ca/ergast/f1/current/last/results.json"))+stamp("Jolpica-F1")}}
        button(f1,t("details"),"f1-results"){detail("f1-results")}
        button(f1,t("favorite_driver"),"f1-results"){
            val names=prefs.getString("latest_f1-results","")!!.lines().filter{it.matches(Regex("^[0-9A-Z]+\\. .+"))}.map{it.substringAfter(". ")}
            if(names.isEmpty())Toast.makeText(activity,t("load_results"),Toast.LENGTH_SHORT).show() else AlertDialog.Builder(activity).setTitle(t("favorite_driver")).setSingleChoiceItems((listOf(t("automatic"))+names).toTypedArray(),0){dialog,i->prefs.edit().putString("f1_favorite",if(i==0)"" else names[i-1]).apply();dialog.dismiss()}.show()
        }
        val schedule=text(t("next_race"),13f);f1.addView(schedule)
        button(f1,t("next_race"),"f1-schedule"){request("f1-schedule",schedule,t("loading")){
            val races=get("https://api.jolpi.ca/ergast/f1/current/next.json").getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races")
            (if(races.length()==0)t("no_schedule") else {val race=races.getJSONObject(0);val date=race.getString("date");val time=race.optString("time");val local=if(time.isNotBlank())DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy · HH:mm z",Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.parse(date+"T"+time))else date;race.getString("raceName")+"\n"+local})+stamp("Jolpica-F1")
        }}
        parent.addView(text(t("result_limit"),12f))
    }
    private fun fetchFootball() {
        val selectedId = teamId
        request("football", footballResult, "Ergebnis wird geladen …") {
            SportResults.football(get("https://www.thesportsdb.com/api/v1/json/123/eventslast.php?id=$selectedId"), selectedId) + stamp("TheSportsDB")
        }
    }
    private fun editTeam() {
        val name = EditText(activity).apply { hint = t("search_hint"); setSingleLine(); setText(team.text) }
        val form = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)); addView(name) }
        val info = text(t("search_info")); form.addView(info)
        val dialog = AlertDialog.Builder(activity).setTitle(t("select_club")).setView(form).setNegativeButton(t("cancel"), null).setPositiveButton(t("search"), null).create()
        dialog.setOnShowListener {
            val search = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            search.setOnClickListener {
                val query = name.text.toString().trim()
                if (query.length !in 2..60) { name.error = t("invalid_team"); return@setOnClickListener }
                if (busy.contains("football")) return@setOnClickListener
                if (!ready("team-search")) return@setOnClickListener
                busy.add("football"); buttons["football"]?.forEach { it.isEnabled = false }; search.isEnabled = false; info.text = t("searching")
                worker.execute {
                    var found: Pair<String, String>? = null
                    val message = try {
                        val teams = get("https://www.thesportsdb.com/api/v1/json/123/searchteams.php?t=" + URLEncoder.encode(query, "UTF-8")).optJSONArray("teams")
                        val candidates = if (teams == null) emptyList() else (0 until teams.length()).map { teams.getJSONObject(it) }
                        val match = candidates.firstOrNull { it.optString("strSport") == "Soccer" && it.optString("idTeam").matches(Regex("[0-9]+")) }
                        if (match == null) t("no_team") else {
                            found = match.getString("idTeam") to match.getString("strTeam"); ""
                        }
                    } catch (_: Exception) { t("network_error") }
                    activity.runOnUiThread {
                        if (!closed) {
                            busy.remove("football"); buttons["football"]?.forEach { it.isEnabled = true }
                            if (dialog.isShowing) {
                                search.isEnabled = true
                                val selected = found
                                if (selected == null) info.text = message else {
                                    dialog.dismiss()
                                    AlertDialog.Builder(activity).setTitle("${selected.second} auswählen?")
                                        .setMessage("Ergebnisse für diesen Fußballverein laden?")
                                        .setNegativeButton(t("cancel"), null).setPositiveButton(t("select")) { _, _ ->
                                            teamId = selected.first; team.text = selected.second
                                            prefs.edit().putString("results_team_id", selected.first).putString("results_team_name", selected.second).remove("latest_football").apply()
                                            footballResult.text = t("load_results")
                                        }.show()
                                }
                            }
                        }
                    }
                }
            }
        }; dialog.show()
    }
    private fun ready(key: String): Boolean {
        if (closed || busy.contains(key)) return false
        val now = SystemClock.elapsedRealtime()
        if (attempted[key]?.let { now - it < 30_000 } == true) {
            Toast.makeText(activity, t("wait_refresh"), Toast.LENGTH_SHORT).show(); return false
        }
        attempted[key] = now; return true
    }
    private fun request(key: String, target: TextView, loading: String, fetch: () -> String) {
        if (!ready(key)) return
        busy.add(key); buttons[key]?.forEach { it.isEnabled = false }; target.text = loading
        worker.execute {
            val value = try { fetch().also { if (key == "football" || key == "f1-results") prefs.edit().putString("latest_$key", it).apply() } } catch (e: Exception) {
                if (e is IllegalStateException) e.message ?: "Daten nicht verfügbar." else t("network_error")
            }
            activity.runOnUiThread {
                if (!closed) { busy.remove(key); buttons[key]?.forEach { it.isEnabled = true }; target.text = compact(key,value) }
            }
        }
    }
    private fun stamp(source: String) = "\n\n"+t("retrieved")+": " + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.getDefault()).withZone(ZoneId.systemDefault()).format(Instant.now()) + " · " + source
    private fun get(address: String): JSONObject {
        val conn = URL(address).openConnection() as HttpsURLConnection
        try {
            conn.connectTimeout = 10_000; conn.readTimeout = 10_000; conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", "AHM3T45Studio/0.9")
            if (conn.responseCode == 429) throw IllegalStateException("Anbieterlimit erreicht. Bitte später erneut versuchen.")
            if (conn.responseCode != 200) throw IllegalStateException("Datenquelle derzeit nicht verfügbar (${conn.responseCode}).")
            val bytes = conn.inputStream.use { input ->
                val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(4096)
                while (output.size() <= 262_144) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 262_145 - output.size()))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            if (bytes.size > 262_144) throw IllegalStateException("Antwort der Datenquelle ist zu groß.")
            return JSONObject(String(bytes, Charsets.UTF_8))
        } finally { conn.disconnect() }
    }
    fun close() { closed = true; worker.shutdownNow() }
}
