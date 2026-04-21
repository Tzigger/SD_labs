package com.sd.laborator.services

import com.sd.laborator.interfaces.GeoFilterInterface
import org.springframework.stereotype.Service
import java.util.TimeZone

/**
 * =====================================================================
 * SERVICIUL DE FILTRARE GEOGRAFICA – pattern ORCHESTRARE (Orchestration)
 * =====================================================================
 *
 * Rol in orchestrare: serviciu INDEPENDENT
 * Nu apeleaza si nu cunoaste niciun alt serviciu.
 *
 * Functionare:
 *   1. Citeste timezone-ul curent al sistemului de operare.
 *   2. Compara cu lista neagra din fisierul 'blacklist.txt'.
 *   3. Expune doua metode simple: isAccessAllowed() si getCurrentZone().
 *
 * Diferenta fata de Chaining:
 *   - In Chaining: GeoFilterService apeleaza direct urmatorul serviciu.
 *   - In Orchestrare: GeoFilterService NU apeleaza niciodata alt serviciu.
 *     El raspunde doar la intrebari si lasa controllerul sa decida ce face.
 */
@Service
class GeoFilterService : GeoFilterInterface {

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

    override fun getCurrentZone(): String = TimeZone.getDefault().id

    override fun isAccessAllowed(): Boolean {
        val zone = getCurrentZone()
        val blocked = blacklistedZones.contains(zone)
        println("[GeoFilterService] Zona detectata: $zone | Blocata: $blocked | Zone blocate: $blacklistedZones")
        return !blocked
    }
}
