package com.sd.laborator.pojo

data class WeatherForecastData(
    var location: String,
    var date: String,
    var weatherState: String,
    var windSpeed: Double,     // km/h
    var minTemp: Double,       // grade celsius
    var maxTemp: Double,
    var currentTemp: Double,
    var humidity: Int          // procent
) {
    override fun toString(): String {
        return """
            |Locatie: $location
            |Data: $date
            |Stare meteo: $weatherState
            |Viteza vant: ${"%.1f".format(windSpeed)} km/h
            |Temperatura minima: ${"%.1f".format(minTemp)} °C
            |Temperatura maxima: ${"%.1f".format(maxTemp)} °C
            |Temperatura curenta: ${"%.1f".format(currentTemp)} °C
            |Umiditate: $humidity%
        """.trimMargin()
    }
}
