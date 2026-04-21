package com.sd.laborator.interfaces

interface LocationSearchInterface {
    /**
     * Cauta coordonatele geografice pentru o locatie data si
     * apeleaza WeatherForecastService
     *
     * @param locationName numele locatiei
     * @return rezultatul final (string formatat) sau mesaj de eroare
     */
    fun searchAndChain(locationName: String): String
}
