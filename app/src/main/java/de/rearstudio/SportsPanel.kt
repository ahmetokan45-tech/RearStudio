package de.rearstudio

import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.InputType
import android.widget.*
import org.json.JSONObject
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

/** External score links stay external; only the explicitly requested Jolpica schedule is fetched. */
class SportsPanel(private val activity: Activity, parent: LinearLayout) {
    private val prefs = activity.getSharedPreferences("sport", Activity.MODE_PRIVATE)
    private val worker = Executors.newSingleThreadExecutor()
    @Volatile private var closed = false
    private var loading = false
    private var lastAttempt = 0L
    private lateinit var schedule: TextView
    private lateinit var refresh: Button
    private fun dp(n: Int) = (n * activity.resources.displayMetrics.density).toInt()
    private fun text(value: String, size: Float = 14f) = TextView(activity).apply {
        text = value; textSize = size; setTextColor(Color.WHITE); setPadding(0, dp(4), 0, dp(8))
    }
    private fun card(parent: LinearLayout, color: Int): LinearLayout = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply { setColor(color); cornerRadius = dp(20).toFloat() }
        parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(16) })
    }
    private fun button(parent: LinearLayout, value: String, action: () -> Unit) = Button(activity).apply {
        text = value; isAllCaps = false; textSize = 14f; minHeight = dp(48)
        setTextColor(Color.WHITE); backgroundTintList = android.content.res.ColorStateList.valueOf(0xff354257.toInt())
        parent.addView(this, LinearLayout.LayoutParams(-1, -2)); setOnClickListener { action() }
    }
    init {
        val football = card(parent, 0xff38242c.toInt())
        val team = text(prefs.getString("team", "Galatasaray") ?: "Galatasaray", 22f); football.addView(team)
        football.addView(text("Dein Verein. Deine Ergebnisquelle."))
        val footballSource = text(sourceHost("football", FOOTBALL), 12f); football.addView(footballSource)
        button(football, "Live-Ergebnisse öffnen ↗") { open(prefs.getString("football", FOOTBALL) ?: FOOTBALL) }
        button(football, "Verein & Link ändern") { edit("football", FOOTBALL, team, footballSource) }
        football.addView(text("Öffnet deine gewählte Website. Noch keine Spielstände innerhalb der App.", 12f))
        val f1 = card(parent, 0xff222d40.toInt())
        f1.addView(text("Formel 1", 22f))
        schedule = text("Nächster Grand Prix\nTippe auf „Renntermin laden“. Es wird vorher keine Verbindung hergestellt.")
        f1.addView(schedule)
        refresh = button(f1, "Renntermin laden") { fetchSchedule() }
        val f1Source = text(sourceHost("f1", F1), 12f); f1.addView(f1Source)
        button(f1, "Live-Timing öffnen ↗") { open(prefs.getString("f1", F1) ?: F1) }
        button(f1, "Timing-Link ändern") { edit("f1", F1, null, f1Source) }
        f1.addView(text("Renntermin: Jolpica-F1. Keine Live-Rundenzeiten. Die verlinkte Seite kann Anmeldung oder Abo verlangen.", 12f))
    }
    private fun sourceHost(key: String, default: String) = "Website: " + (Uri.parse(prefs.getString(key, default)).host ?: "nicht eingestellt")
    private fun open(url: String) {
        try { activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: ActivityNotFoundException) { Toast.makeText(activity, "Kein Browser installiert.", Toast.LENGTH_LONG).show() }
    }
    private fun edit(key: String, default: String, team: TextView?, source: TextView) {
        val form = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        val name = EditText(activity).apply { hint = "Dein Verein"; setSingleLine(); setText(prefs.getString("team", "Galatasaray")) }
        if (team != null) { form.addView(text("Verein")); form.addView(name) }
        form.addView(text("HTTPS-Link zur gewünschten Ergebnisseite"))
        val link = EditText(activity).apply { inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI; setText(prefs.getString(key, default)); maxLines = 3 }
        form.addView(link)
        val dialog = AlertDialog.Builder(activity).setTitle("Sport anpassen").setView(form)
            .setNegativeButton("Abbrechen", null).setNeutralButton("Standard", null).setPositiveButton("Speichern", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { link.setText(default); if (team != null) name.setText("Galatasaray") }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val url = link.text.toString().trim()
                if (!SportsLink.isValid(url)) { link.error = "Bitte einen vollständigen HTTPS-Link ohne Zugangsdaten eingeben."; return@setOnClickListener }
                if (team != null && name.text.toString().isBlank()) { name.error = "Bitte einen Vereinsnamen eingeben."; return@setOnClickListener }
                prefs.edit().putString(key, url).apply()
                if (team != null) { val value = name.text.toString().trim().take(60); prefs.edit().putString("team", value).apply(); team.text = value }
                source.text = sourceHost(key, default); dialog.dismiss()
            }
        }; dialog.show()
    }
    private fun fetchSchedule() {
        if (loading || closed) return
        if (System.currentTimeMillis() - lastAttempt < 30_000) {
            Toast.makeText(activity, "Bitte 30 Sekunden bis zur nächsten Anfrage warten.", Toast.LENGTH_SHORT).show(); return
        }
        lastAttempt = System.currentTimeMillis(); loading = true; refresh.isEnabled = false; schedule.text = "Renntermin wird geladen …"
        worker.execute {
            val result = try {
                val conn = URL("https://api.jolpi.ca/ergast/f1/current/next.json").openConnection() as HttpsURLConnection
                try {
                    conn.connectTimeout = 10_000; conn.readTimeout = 10_000; conn.instanceFollowRedirects = false
                    conn.setRequestProperty("User-Agent", "AHM3T45Studio/0.6")
                    if (conn.responseCode == 429) throw IllegalStateException("Anbieterlimit erreicht. Bitte später erneut versuchen.")
                    if (conn.responseCode != 200) throw IllegalStateException("Datenquelle derzeit nicht verfügbar (${conn.responseCode}).")
                    val bytes = conn.inputStream.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(4096)
                        while (output.size() <= 262_144) {
                            val count = input.read(buffer, 0, minOf(buffer.size, 262_145 - output.size()))
                            if (count < 0) break
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    if (bytes.size > 262_144) throw IllegalStateException("Antwort der Datenquelle ist zu groß.")
                    val races = JSONObject(String(bytes, Charsets.UTF_8)).getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races")
                    if (races.length() == 0) "Kein nächstes Rennen veröffentlicht."
                    else {
                        val race = races.getJSONObject(0)
                        val date = race.getString("date")
                        val time = race.optString("time")
                        val local = if (time.isNotBlank()) DateTimeFormatter.ofPattern("EEE, dd.MM.yyyy · HH:mm z", Locale.GERMAN).withZone(ZoneId.systemDefault()).format(Instant.parse(date + "T" + time)) else date
                        race.getString("raceName") + "\n" + local + "\n" + race.getJSONObject("Circuit").getString("circuitName")
                    } + "\n\nAbgerufen: " + DateTimeFormatter.ofPattern("dd.MM. HH:mm", Locale.GERMAN).withZone(ZoneId.systemDefault()).format(Instant.now()) + " · Jolpica-F1"
                } finally { conn.disconnect() }
            } catch (e: Exception) { if (e is IllegalStateException) e.message ?: "Daten nicht verfügbar." else "Keine Daten geladen. Bitte Internetverbindung prüfen und erneut versuchen." }
            activity.runOnUiThread {
                if (!closed) { loading = false; refresh.isEnabled = true; refresh.text = "Renntermin aktualisieren"; schedule.text = result }
            }
        }
    }
    fun close() { closed = true; worker.shutdownNow() }
    companion object {
        const val FOOTBALL = "https://www.livescore.com/en/football/"
        const val F1 = "https://www.formula1.com/en/timing/f1-live"
    }
}
