package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherForecastData

interface WeatherForecastInterface {
    /**
     * Obtine datele meteo pentru coordonatele geografice date.
     *
     * @param latitude  latitudinea locatiei
     * @param longitude longitudinea locatiei
     * @param locationName denumirea afisabila a locatiei
     * @return obiectul POJO cu datele meteo
     */
    fun getForecastData(latitude: Double, longitude: Double, locationName: String): WeatherForecastData
}
