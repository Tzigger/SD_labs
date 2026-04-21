package com.sd.laborator.interfaces

interface LocationSearchInterface {
    /**
     * Obtine coordonatele geografice (si numele normalizat) pentru
     * o locatie data prin denumire.
     *
     * @param locationName numele locatiei (oras, tara, etc.)
     * @return un obiect LocationData cu lat/lon/displayName, sau null daca nu e gasita
     */
    fun getLocationCoordinates(locationName: String): LocationData?
}

/**
 * Data class simpla ce incapsuleaza rezultatul geocodarii.
 */
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val displayName: String
)
