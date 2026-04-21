package com.sd.laborator.interfaces

interface GeoFilterInterface {
    /**
     * Verifica daca zona geografica curenta este in lista neagra.
     *
     * @return true daca accesul este PERMIS, false daca este BLOCAT
     */
    fun isAccessAllowed(): Boolean

    /**
     * Returneaza ID-ul (timezone-ul) zonei geografice curente detectate.
     */
    fun getCurrentZone(): String
}
