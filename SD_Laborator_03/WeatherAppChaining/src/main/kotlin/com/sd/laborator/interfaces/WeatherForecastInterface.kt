package com.sd.laborator.interfaces

import com.sd.laborator.pojo.WeatherForecastData

//utimul serviciu din lant, returneaza datele.
interface WeatherForecastInterface {
    /**
     * Obtine datele meteo pe baza coordonatelor geografice.
     *
     * @param latitude  latitudinea locatiei
     * @param longitude longitudinea locatiei
     * @param locationName denumirea afisabila a locatiei
     * @return obiect WeatherForecastData cu datele meteo
     */
    fun getForecastData(latitude: Double, longitude: Double, locationName: String): WeatherForecastData
}
