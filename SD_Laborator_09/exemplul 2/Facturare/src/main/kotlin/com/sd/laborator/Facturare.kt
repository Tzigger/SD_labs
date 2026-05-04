package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.integration.annotation.Transformer
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

@EnableBinding(Processor::class)
@SpringBootApplication
class FacturareMicroservice {
    companion object {
        private val bazaDateFolder = File(System.getProperty("java.io.tmpdir"), "sd_lab09_db")
        private val facturiFile = File(bazaDateFolder, "facturi.txt")

        private fun initializareBazaDate() {
            if (!bazaDateFolder.exists()) {
                bazaDateFolder.mkdirs()
            }

            if (!facturiFile.exists()) {
                facturiFile.createNewFile()
            }
        }

        private fun inregistreazaFactura(nrFactura: Int, idComanda: Int) {
            initializareBazaDate()
            facturiFile.appendText("$nrFactura|$idComanda\n")
            println("Factura $nrFactura pentru comanda $idComanda a fost inregistrata in baza de date text.")
        }
    }

    @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    ///TODO - parametrul ar trebui sa fie doar numarul de inregistrare al comenzii si atat
    ///DONE - metoda primeste doar ID-ul comenzii din flux
    fun emitereFactura(comanda: String?): String {
        val idComanda = comanda!!.trim().toInt()
        println("Emit factura pentru comanda $idComanda...")
        val nrFactura = abs(Random.nextInt())
        println("S-a emis factura cu nr $nrFactura.")

        ///TODO - inregistrare factura in baza de date
        ///DONE - factura este persistata in fisierul facturi.txt
        inregistreazaFactura(nrFactura, idComanda)

        return "$idComanda"
    }
}

fun main(args: Array<String>) {
    runApplication<FacturareMicroservice>(*args)
}
