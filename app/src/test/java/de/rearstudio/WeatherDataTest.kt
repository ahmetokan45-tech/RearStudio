package de.rearstudio
import org.junit.Assert.*
import org.junit.Test
class WeatherDataTest {
 @Test fun realSchemaParsesUnitsAndTimestamp(){val r=WeatherData.parse("""{"current":{"temperature_2m":12.4,"relative_humidity_2m":72,"wind_speed_10m":8.1,"weather_code":45,"time":"2026-11-07T13:30"}}""");assertEquals(12.4,r.temperature,.01);assertEquals("weather_fog",WeatherData.condition(r.code));assertEquals("2026-11-07T13:30",r.observed)}
 @Test fun incompletePayloadRejected(){assertThrows(Exception::class.java){WeatherData.parse("""{"current":{"temperature_2m":12}}""")}}
 @Test fun conditionGroups(){assertEquals("weather_snow",WeatherData.condition(85));assertEquals("weather_storm",WeatherData.condition(99));assertEquals("weather_unknown",WeatherData.condition(123))}
}
