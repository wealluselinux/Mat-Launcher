package com.matlauncher.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

data class HourForecast(val label: String, val tempC: Double, val code: Int, val isDay: Boolean, val precipPct: Int)

data class DayForecast(val date: String, val code: Int, val hiC: Double, val loC: Double)

data class WeatherData(
    val tempC: Double,
    val feelsC: Double,
    val code: Int,
    val humidity: Int,
    val windKmh: Double,
    val isDay: Boolean,
    val precipPct: Int,
    val days: List<DayForecast>,
    val hours: List<HourForecast> = emptyList(),
)

data class GeoResult(val name: String, val admin: String, val country: String, val lat: Double, val lon: Double) {
    val display: String get() = listOf(name, admin, country).filter { it.isNotBlank() }.joinToString(", ")
}

fun tempString(c: Double, fahrenheit: Boolean): String =
    (if (fahrenheit) (c * 9 / 5 + 32) else c).roundToInt().toString() + "°"

fun conditionLabel(code: Int): String = when (code) {
    0 -> "Clear sky"
    1 -> "Mostly clear"
    2 -> "Partly cloudy"
    3 -> "Overcast"
    45, 48 -> "Fog"
    51, 53, 55 -> "Drizzle"
    56, 57 -> "Freezing drizzle"
    61 -> "Light rain"
    63 -> "Rain"
    65 -> "Heavy rain"
    66, 67 -> "Freezing rain"
    71 -> "Light snow"
    73 -> "Snow"
    75 -> "Heavy snow"
    77 -> "Snow grains"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Unknown"
}

object WeatherApi {
    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 10_000
        conn.setRequestProperty("User-Agent", "MatLauncher/1.0")
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun fetch(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,wind_speed_10m,is_day" +
                "&hourly=temperature_2m,weather_code,precipitation_probability,is_day" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&timezone=auto&forecast_days=7"
            val o = JSONObject(get(url))
            val cur = o.getJSONObject("current")
            val d = o.getJSONObject("daily")
            val dates = d.getJSONArray("time")
            val codes = d.getJSONArray("weather_code")
            val his = d.getJSONArray("temperature_2m_max")
            val los = d.getJSONArray("temperature_2m_min")
            val precip = d.optJSONArray("precipitation_probability_max")
            val days = (0 until dates.length()).map {
                DayForecast(dates.getString(it), codes.getInt(it), his.getDouble(it), los.getDouble(it))
            }
            val hours = try {
                val h = o.getJSONObject("hourly")
                val times = h.getJSONArray("time")
                val ht = h.getJSONArray("temperature_2m")
                val hc = h.getJSONArray("weather_code")
                val hp = h.optJSONArray("precipitation_probability")
                val hd = h.optJSONArray("is_day")
                val now = cur.optString("time").take(13)
                val start = (0 until times.length()).firstOrNull { times.getString(it).take(13) >= now } ?: 0
                (start until minOf(start + 24, times.length())).mapIndexed { n, i ->
                    val hr = times.getString(i).substring(11, 13).toInt()
                    val label = if (n == 0) "Now" else "${if (hr % 12 == 0) 12 else hr % 12} ${if (hr < 12) "AM" else "PM"}"
                    HourForecast(
                        label,
                        ht.getDouble(i),
                        hc.getInt(i),
                        hd == null || hd.optInt(i, 1) == 1,
                        if (hp != null && !hp.isNull(i)) hp.getInt(i) else 0,
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
            WeatherData(
                tempC = cur.getDouble("temperature_2m"),
                feelsC = cur.getDouble("apparent_temperature"),
                code = cur.getInt("weather_code"),
                humidity = cur.getInt("relative_humidity_2m"),
                windKmh = cur.getDouble("wind_speed_10m"),
                isDay = cur.getInt("is_day") == 1,
                precipPct = if (precip != null && precip.length() > 0 && !precip.isNull(0)) precip.getInt(0) else 0,
                days = days,
                hours = hours,
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun search(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        try {
            val q = URLEncoder.encode(query.trim(), "UTF-8")
            val o = JSONObject(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=en&format=json"))
            val arr = o.optJSONArray("results") ?: return@withContext emptyList()
            (0 until arr.length()).map {
                val r = arr.getJSONObject(it)
                GeoResult(
                    name = r.optString("name"),
                    admin = r.optString("admin1"),
                    country = r.optString("country"),
                    lat = r.getDouble("latitude"),
                    lon = r.getDouble("longitude"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
