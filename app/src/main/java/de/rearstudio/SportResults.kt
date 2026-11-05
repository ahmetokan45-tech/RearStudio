package de.rearstudio

import org.json.JSONObject

/** Missing scores remain missing; a numeric 0 is a real score, not a fallback. */
object SportResults {
    private fun JSONObject.clean(key: String): String = if (isNull(key)) "" else optString(key).trim()
    fun football(json: JSONObject, teamId: String): String {
        val events = json.optJSONArray("results") ?: return "Kein Ergebnis für diesen Verein verfügbar."
        val matching = (0 until events.length()).map { events.getJSONObject(it) }
            .filter { it.clean("idHomeTeam") == teamId || it.clean("idAwayTeam") == teamId }
            .sortedByDescending { it.clean("dateEvent") }
        val event = matching.firstOrNull() ?: return "Kein passendes Ergebnis vom Anbieter geliefert."
        val home = event.clean("intHomeScore").takeIf { it.matches(Regex("[0-9]+")) }
        val away = event.clean("intAwayScore").takeIf { it.matches(Regex("[0-9]+")) }
        val score = if (home != null && away != null) "$home : $away" else "Spielstand nicht veröffentlicht"
        val status = when (val value = event.clean("strStatus")) {
            "FT", "AET", "PEN", "Match Finished" -> "Beendet"
            "NS", "Not Started" -> "Noch nicht begonnen"
            "PST", "Postponed" -> "Verschoben"
            "" -> "Status nicht angegeben"
            else -> "Anbieterstatus: $value (kein Live-Feed)"
        }
        return "${event.clean("strHomeTeam")}\n$score\n${event.clean("strAwayTeam")}\n\n${event.clean("dateEvent")} · $status\n${event.clean("strLeague")}"
    }
    fun f1(json: JSONObject): String {
        val races = json.getJSONObject("MRData").getJSONObject("RaceTable").getJSONArray("Races")
        if (races.length() == 0) return "Noch kein Rennergebnis veröffentlicht."
        val race = races.getJSONObject(0)
        val results = race.optJSONArray("Results")
        if (results == null || results.length() == 0) return "Noch kein Rennergebnis veröffentlicht."
        val rows = (0 until results.length()).map { i ->
            val result = results.getJSONObject(i)
            val driver = result.getJSONObject("Driver")
            val position = result.clean("positionText").ifBlank { result.clean("position") }
            val detail = result.optJSONObject("Time")?.clean("time")?.takeIf { it.isNotEmpty() }
                ?: result.clean("status")
            "$position. ${driver.clean("givenName")} ${driver.clean("familyName")}\n${result.getJSONObject("Constructor").clean("name")} · ${result.clean("points")} Pkt. · $detail"
        }
        return race.getString("raceName") + "\n" + race.getString("date") + " · Veröffentlichtes Rennergebnis\n\n" + rows.joinToString("\n\n")
    }
}
