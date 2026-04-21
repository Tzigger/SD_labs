package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationData
import com.sd.laborator.interfaces.LocationSearchInterface
import org.json.JSONObject
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * =====================================================================
 * SERVICIUL DE CAUTARE A LOCATIEI – pattern ORCHESTRARE (Orchestration)
 * =====================================================================
 *
 * Rol in orchestrare: serviciu INDEPENDENT
 * Nu apeleaza si nu cunoaste niciun alt serviciu.
 *
 * Functionare:
 *   1. Primeste un nume de locatie.
 *   2. Interogheaza Open-Meteo Geocoding API.
 *   3. Returneaza un obiect LocationData (lat, lon, displayName) sau null.
 *   4. NU formateaza niciun raspuns final – asta e treaba controllerului.
 *
 * Diferenta fata de Chaining:
 *   - In Chaining: LocationSearchService apeleaza WeatherForecastService.
 *   - In Orchestrare: LocationSearchService NU stie de WeatherForecastService.
 *     Returneaza doar datele brute si asteapta ca orchestratorul (controllerul)
 *     sa le foloseasca cum doreste.
 *
 * API utilizat: https://geocoding-api.open-meteo.com/v1/search
 */
@Service
class LocationSearchService : LocationSearchInterface {

    override fun getLocationCoordinates(locationName: String): LocationData? {
        println("[LocationSearchService] Geocodare locatie: $locationName")

        val encodedName = URLEncoder.encode(locationName, StandardCharsets.UTF_8.toString())
        val searchUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedName&count=1&language=en&format=json")

        val rawResponse: String = try {
            val conn = searchUrl.openConnection()
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 SD-Lab/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            println("[LocationSearchService] Eroare la geocodare: ${e.message}")
            return null
        }

        val root = JSONObject(rawResponse)

        if (!root.has("results") || root.isNull("results")) {
            println("[LocationSearchService] Locatia nu a fost gasita.")
            return null
        }

        val results = root.getJSONArray("results")
        if (results.length() == 0) {
            println("[LocationSearchService] Niciun rezultat returnat.")
            return null
        }

        val firstResult = results.getJSONObject(0)
        val latitude  = firstResult.getDouble("latitude")
        val longitude = firstResult.getDouble("longitude")
        val displayName = buildString {
            append(firstResult.getString("name"))
            if (firstResult.has("admin1") && !firstResult.isNull("admin1")) {
                append(", ").append(firstResult.getString("admin1"))
            }
            if (firstResult.has("country") && !firstResult.isNull("country")) {
                append(", ").append(firstResult.getString("country"))
            }
        }

        println("[LocationSearchService] Locatie gasita: $displayName ($latitude, $longitude)")
        return LocationData(latitude, longitude, displayName)
    }
}
