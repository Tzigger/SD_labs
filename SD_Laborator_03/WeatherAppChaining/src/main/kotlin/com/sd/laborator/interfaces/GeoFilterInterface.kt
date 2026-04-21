package com.sd.laborator.interfaces

interface GeoFilterInterface {
    /**
     * Verifica daca zona geografica curenta este permisa.
     * Daca DA  -> apeleaza LocationSearchInterface
     * Daca NU  -> returneaza un mesaj de acces refuzat.
     *
     * @param location numele locatiei pentru care se solicita prognoza
     * @return rezultatul final al lantului de servicii sau mesaj de eroare
     */
    fun requestForecast(location: String): String
}
