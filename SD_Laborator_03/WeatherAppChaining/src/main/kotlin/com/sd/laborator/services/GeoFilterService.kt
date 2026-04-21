package com.sd.laborator.services

import com.sd.laborator.interfaces.GeoFilterInterface
import com.sd.laborator.interfaces.LocationSearchInterface
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.TimeZone

/**
 * =====================================================================
 * SERVICIUL DE FILTRARE GEOGRAFICA – pattern INLANTUIRE (Chaining)
 * =====================================================================
 *
 * Rol in lant:  PRIMUL  (HEAD of chain)
 * Apeleaza:     LocationSearchService (urmatorul in lant)
 *
 * Functionare:
 *   1. Citeste timezone-ul curent al sistemului de operare.
 *   2. Compara cu lista neagra din fisierul 'blacklist.txt'.
 *   3. Daca zona este BLOCATA  -> returneaza mesaj de acces refuzat.
 *   4. Daca zona este PERMISA  -> paseaza cererea catre LocationSearchService.
 *
 * Principiu de inlantuire:
 *   GeoFilterService NU stie nimic despre WeatherForecastService.
 *   Stie doar ca exista un urmator in lant (LocationSearchInterface)
 *   caruia ii paseaza cererea dupa validare.
 */
@Service
class GeoFilterService : GeoFilterInterface {

    @Autowired
    private lateinit var locationSearchService: LocationSearchInterface

    // Lista zonelor blocate, incarcata o singura data la pornirea aplicatiei
    private val blacklistedZones: Set<String> by lazy {
        val resource = GeoFilterService::class.java.classLoader
            .getResourceAsStream("blacklist.txt")
            ?: return@lazy emptySet()
        resource.bufferedReader()
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()
    }

    override fun requestForecast(location: String): String {
        // Determinarea timezone-ului curent al nodului de calcul
        val currentTimezone = TimeZone.getDefault().id
        println("[GeoFilterService] Zona geografica detectata: $currentTimezone")
        println("[GeoFilterService] Zone blocate: $blacklistedZones")

        // Verificare in lista neagra
        if (blacklistedZones.contains(currentTimezone)) {
            println("[GeoFilterService] Acces REFUZAT pentru zona: $currentTimezone")
            return "ACCES REFUZAT: Aplicatia nu este disponibila pentru zona geografica " +
                   "\"$currentTimezone\". Va rugam contactati administratorul."
        }

        println("[GeoFilterService] Acces PERMIS. Lantul continua catre LocationSearchService...")
        // Accesul e permis -> se paseaza cererea catre urmatorul serviciu din lant
        return locationSearchService.searchAndChain(location)
    }
}
