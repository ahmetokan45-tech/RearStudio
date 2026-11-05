package de.rearstudio

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SportResultsTest {
    private fun football(scores: String, team: String = "133804") = JSONObject("""{"results":[{"idHomeTeam":"$team","idAwayTeam":"2","strHomeTeam":"Galatasaray","strAwayTeam":"Opponent","dateEvent":"2026-01-01","strStatus":"FT",$scores}]}""")
    @Test fun nullScoresDoNotBecomeZero() {
        val value = SportResults.football(football("\"intHomeScore\":null,\"intAwayScore\":null"), "133804")
        assertTrue(value.contains("nicht veröffentlicht")); assertFalse(value.contains("0 : 0"))
    }
    @Test fun realZeroScoresRemainVisible() {
        assertTrue(SportResults.football(football("\"intHomeScore\":\"0\",\"intAwayScore\":\"0\""), "133804").contains("0 : 0"))
    }
    @Test fun otherTeamsAreNeverShownAsFavorite() {
        assertTrue(SportResults.football(football("\"intHomeScore\":2,\"intAwayScore\":1", "999"), "133804").contains("Kein passendes"))
    }
    @Test fun emptyResultsAreExplicit() {
        assertTrue(SportResults.football(JSONObject("""{"results":null}"""), "133804").contains("Kein Ergebnis"))
        assertTrue(SportResults.f1(JSONObject("""{"MRData":{"RaceTable":{"Races":[]}}}""")).contains("Noch kein"))
    }
    @Test fun f1ShowsRetirementAndPointsWithoutInventingTiming() {
        val value = SportResults.f1(JSONObject("""{"MRData":{"RaceTable":{"Races":[{"raceName":"Example GP","date":"2026-01-01","Results":[{"position":"20","positionText":"R","points":"0","status":"Engine","Driver":{"givenName":"Test","familyName":"Driver"},"Constructor":{"name":"Team"}}]}]}}}"""))
        assertTrue(value.contains("R. Test Driver")); assertTrue(value.contains("0 Pkt. · Engine")); assertTrue(value.contains("2026-01-01"))
    }
}
