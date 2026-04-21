package com.sd.laborator.controllers

import com.sd.laborator.interfaces.GeoFilterInterface
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Fluxul complet al lantului:
 *   HTTP GET /getforecast/{location}
 *       -> WeatherAppController
 *           -> GeoFilterService      (verifica zona geografica)
 *               -> LocationSearchService  (geocodeaza orasul)
 *                   -> WeatherForecastService  (obtine prognoza meteo)
 *                   <- WeatherForecastData
 *               <- String formatat
 *           <- String formatat
 *       <- HTTP Response
 */
@Controller
class WeatherAppController {

    @Autowired
    private lateinit var geoFilterService: GeoFilterInterface

    @RequestMapping("/getforecast/{location}", method = [RequestMethod.GET])
    @ResponseBody
    fun getForecast(@PathVariable location: String): String {
        println("[WeatherAppController] Cerere primita pentru: $location")
        println("[WeatherAppController] Se initiaza lantul de servicii...")

        // Controllerul apeleaza DOAR GeoFilterService
        // Restul lantului este gestionat intern de servicii.
        return geoFilterService.requestForecast(location)
    }
}
