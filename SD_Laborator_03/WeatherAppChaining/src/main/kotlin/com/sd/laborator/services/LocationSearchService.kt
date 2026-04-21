package com.sd.laborator.services

import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.interfaces.WeatherForecastInterface
import org.json.JSONArray
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * =====================================================================
 * SERVICIUL DE CAUTARE A LOCATIEI – pattern INLANTUIRE (Chaining)
 * =====================================================================
 *
 * Rol in lant:  AL DOILEA  (middle of chain)
 * Primeste de la: GeoFilterService
 * Apeleaza:       WeatherForecastService (urmatorul in lant)
 *
 * Functionare:
 *   1. Primeste numele locatiei de la GeoFilterService.
 *   2. Interogheaza Open-Meteo Geocoding API pentru a obtine
 *      latitudinea, longitudinea si denumirea normalizata a locatiei.
 *   3. Daca locatia nu e gasita -> returneaza mesaj de eroare.
 *   4. Daca locatia e gasita -> paseaza coordonatele catre WeatherForecastService.
 *
 * Principiu de inlantuire:
 *   LocationSearchService NU stie nimic despre GeoFilterService.
 *   Stie doar ca exista un urmator in lant (WeatherForecastInterface)
 *   caruia ii paseaza coordonatele dupa geocodare.
 *
 * API utilizat: https://geocoding-api.open-meteo.com/v1/search
 */
@Service
class LocationSearchService : LocationSearchInterface {

    @Autowired
    private lateinit var weatherForecastService: WeatherForecastInterface

    override fun searchAndChain(locationName: String): String {
        println("[LocationSearchService] Cautare locatie: $locationName")

        val encodedName = URLEncoder.encode(locationName, StandardCharsets.UTF_8.toString())
        val searchUrl = URL("https://geocoding-api.open-meteo.com/v1/search?name=$encodedName&count=1&language=en&format=json")

        val rawResponse: String = try {
            val conn = searchUrl.openConnection()
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 SD-Lab/1.0")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            return "Eroare la conectarea la serviciul de geocodare: ${e.message}"
        }

        // Open-Meteo returneaza {"results": [...]} sau {"results": null} daca nu gaseste
        val root = org.json.JSONObject(rawResponse)

        if (!root.has("results") || root.isNull("results")) {
            return "Nu s-au putut gasi date pentru locatia \"$locationName\"!"
        }

        val results = root.getJSONArray("results")
        if (results.length() == 0) {
            return "Nu s-au putut gasi date pentru locatia \"$locationName\"!"
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

        println("[LocationSearchService] Locatie gasita: $displayName (lat=$latitude, lon=$longitude)")
        println("[LocationSearchService] Lantul continua catre WeatherForecastService...")

        // Pasare catre urmatorul in lant
        val forecastData = weatherForecastService.getForecastData(latitude, longitude, displayName)
        return forecastData.toString()
    }
}
