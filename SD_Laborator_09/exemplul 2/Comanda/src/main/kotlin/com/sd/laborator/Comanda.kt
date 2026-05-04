package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.integration.annotation.Transformer
import java.io.File
import kotlin.random.Random

@EnableBinding(Processor::class)
@SpringBootApplication
class ComandaMicroservice {
    companion object {
        private val bazaDateFolder = File(System.getProperty("java.io.tmpdir"), "sd_lab09_db")
        private val comenziFile = File(bazaDateFolder, "comenzi.txt")

        private fun initializareBazaDate() {
            if (!bazaDateFolder.exists()) {
                bazaDateFolder.mkdirs()
            }

            if (!comenziFile.exists()) {
                comenziFile.createNewFile()
            }
        }

        private fun inregistreazaComandaInFisier(idComanda: Int, produs: String, cantitate: Int) {
            initializareBazaDate()
            comenziFile.appendText("$idComanda|$produs|$cantitate\n")
            println("Comanda $idComanda a fost inregistrata in baza de date text.")
        }
    }

    private fun pregatireComanda(produs: String, cantitate: Int): Int {
        println("Se pregateste comanda $cantitate x \"$produs\"...")

        ///TODO - asignare numar de inregistrare
        ///DONE - ID-ul comenzii se genereaza aleator si pozitiv
        ///TODO - inregistrare comanda in baza de date
        ///DONE - comanda se persista in comenzi.txt
        val idComanda = Random.nextInt(1, Int.MAX_VALUE)
        inregistreazaComandaInFisier(idComanda, produs, cantitate)
        return idComanda
    }

    @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    fun preluareComanda(comanda: String?): String {
        val (identitateClient, produsComandat, cantitate, adresaLivrare) = comanda!!.split("|")
        println("Am primit comanda urmatoare: ")
        println("$identitateClient | $produsComandat | $cantitate | $adresaLivrare")

        val idComanda = pregatireComanda(produsComandat, cantitate.toInt())

        ///TODO - in loc sa se trimita mesajul cu toate datele in continuare, trebuie trimis doar ID-ul comenzii
        ///DONE - in flux este trimis mai departe doar identificatorul comenzii
        return "$idComanda"
    }
}

fun main(args: Array<String>) {
    runApplication<ComandaMicroservice>(*args)
}
