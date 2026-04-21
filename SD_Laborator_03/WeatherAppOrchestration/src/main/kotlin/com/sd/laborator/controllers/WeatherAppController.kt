package com.sd.laborator.controllers

import com.sd.laborator.interfaces.GeoFilterInterface
import com.sd.laborator.interfaces.LocationSearchInterface
import com.sd.laborator.interfaces.WeatherForecastInterface
import com.sd.laborator.pojo.WeatherForecastData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.ResponseBody

/**
 * Fluxul complet al orchestrarii:
 *   HTTP GET /getforecast/{location}
 *       -> WeatherAppController (ORCHESTRATORUL)
 *           -> PASUL 1: GeoFilterService.isAccessAllowed()
 *              [daca NU] -> returneaza mesaj de acces refuzat
 *           -> PASUL 2: LocationSearchService.getLocationCoordinates(location)
 *              [daca null] -> returneaza mesaj de locatie negasita
 *           -> PASUL 3: WeatherForecastService.getForecastData(lat, lon, name)
 *           -> PASUL 4: controller formateaza si returneaza rezultatul final
 *       <- HTTP Response
 */
@Controller
class WeatherAppController {

    @Autowired
    private lateinit var geoFilterService: GeoFilterInterface

    @Autowired
    private lateinit var locationSearchService: LocationSearchInterface

    @Autowired
    private lateinit var weatherForecastService: WeatherForecastInterface

    @RequestMapping("/getforecast/{location}", method = [RequestMethod.GET])
    @ResponseBody
    fun getForecast(@PathVariable location: String): String {
        println("[WeatherAppController] === ORCHESTRARE INITIATA ===")
        println("[WeatherAppController] Cerere pentru: $location")

        println("[WeatherAppController] PASUL 1: Verificare filtru geografic...")
        if (!geoFilterService.isAccessAllowed()) {
            val zone = geoFilterService.getCurrentZone()
            println("[WeatherAppController] Acces REFUZAT pentru zona: $zone")
            return "ACCES REFUZAT: Aplicatia nu este disponibila pentru zona geografica " +
                   "\"$zone\". Va rugam contactati administratorul."
        }
        println("[WeatherAppController] PASUL 1: Acces PERMIS.")

        println("[WeatherAppController] PASUL 2: Geocodare locatie \"$location\"...")
        val locationData = locationSearchService.getLocationCoordinates(location)
        if (locationData == null) {
            println("[WeatherAppController] Locatia nu a fost gasita.")
            return "Nu s-au putut gasi date meteo pentru cuvintele cheie \"$location\"!"
        }
        println("[WeatherAppController] PASUL 2: Locatie gasita -> ${locationData.displayName}")

        println("[WeatherAppController] PASUL 3: Obtinere prognoza meteo...")
        val forecastData: WeatherForecastData = weatherForecastService.getForecastData(
            latitude     = locationData.latitude,
            longitude    = locationData.longitude,
            locationName = locationData.displayName
        )
        println("[WeatherAppController] PASUL 3: Date meteo obtinute.")

        println("[WeatherAppController] PASUL 4: Formatare raspuns final.")
        println("[WeatherAppController] === ORCHESTRARE FINALIZATA ===")
        return forecastData.toString()
    }
}
