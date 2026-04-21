package com.sd.laborator.services

import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.WeatherForecastData
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL

/**
 * =====================================================================
 * SERVICIUL DE PROGNOZA METEO – pattern ORCHESTRARE (Orchestration)
 * =====================================================================
 *
 * Rol in orchestrare: serviciu INDEPENDENT
 * Nu apeleaza si nu cunoaste niciun alt serviciu.
 *
 * Functionare:
 *   1. Primeste coordonate geografice + numele locatiei.
 *   2. Interogheaza Open-Meteo Forecast API.
 *   3. Construieste si returneaza obiectul POJO WeatherForecastData.
 *   4. NU formateaza niciun raspuns final – asta e treaba controllerului.
 *
 * Diferenta fata de Chaining:
 *   - In Chaining: WeatherForecastService returneaza la capatul lantului.
 *   - In Orchestrare: WeatherForecastService returneaza POJO-ul direct
 *     catre controller (orchestratorul), care decide cum sa il formateze.
 *
 * API utilizat: https://api.open-meteo.com/v1/forecast
 */
@Service
class WeatherForecastService(private val timeService: TimeService) : WeatherForecastInterface {

    override fun getForecastData(latitude: Double, longitude: Double, locationName: String): WeatherForecastData {
        println("[WeatherForecastService] Obtinere date meteo: $locationName ($latitude, $longitude)")

        val forecastUrl = URL(
            "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$latitude&longitude=$longitude" +
            "&current_weather=true" +
            "&hourly=relativehumidity_2m,temperature_2m,precipitation_probability,weathercode" +
            "&daily=weathercode,temperature_2m_max,temperature_2m_min,windspeed_10m_max" +
            "&timezone=auto" +
            "&forecast_days=1"
        )

        val rawResponse: String = try {
            val conn = forecastUrl.openConnection()
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 SD-Lab/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            println("[WeatherForecastService] Eroare API meteo: ${e.message}")
            return WeatherForecastData(locationName, timeService.getCurrentTime(), "Eroare API (Time out)", 0.0, 0.0, 0.0, 0.0, 0)
        }
        
        val root = JSONObject(rawResponse)

        val currentWeather = root.getJSONObject("current_weather")
        val currentTemp    = currentWeather.getDouble("temperature")
        val windSpeed      = currentWeather.getDouble("windspeed")
        val weatherCode    = currentWeather.getInt("weathercode")

        val daily   = root.getJSONObject("daily")
        val minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0)
        val maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0)

        val hourly    = root.getJSONObject("hourly")
        val humidityArr = hourly.getJSONArray("relativehumidity_2m")
        var humiditySum = 0
        val count = minOf(humidityArr.length(), 24)
        for (i in 0 until count) humiditySum += humidityArr.getInt(i)
        val avgHumidity = if (count > 0) humiditySum / count else 0

        println("[WeatherForecastService] Date meteo obtinute. Returneaza POJO catre orchestrator (controller).")

        return WeatherForecastData(
            location     = locationName,
            date         = timeService.getCurrentTime(),
            weatherState = weatherCodeToDescription(weatherCode),
            windSpeed    = windSpeed,
            minTemp      = minTemp,
            maxTemp      = maxTemp,
            currentTemp  = currentTemp,
            humidity     = avgHumidity
        )
    }

    private fun weatherCodeToDescription(code: Int): String = when (code) {
        0            -> "Cer senin"
        1            -> "In mare parte senin"
        2            -> "Partial noros"
        3            -> "Innoptat"
        45, 48       -> "Ceata"
        51, 53, 55   -> "Burnitoare"
        61, 63, 65   -> "Ploaie"
        71, 73, 75   -> "Ninsoare"
        77           -> "Granule de zapada"
        80, 81, 82   -> "Averse de ploaie"
        85, 86       -> "Averse de zapada"
        95           -> "Furtuna"
        96, 99       -> "Furtuna cu grindina"
        else         -> "Conditii meteo necunoscute (cod $code)"
    }
}
