package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Processor
import org.springframework.integration.annotation.Transformer
import java.io.File

@EnableBinding(Processor::class)
@SpringBootApplication
class DepozitMicroservice {
    companion object {
        private val bazaDateFolder = File(System.getProperty("java.io.tmpdir"), "sd_lab09_db")
        private val stocFile = File(bazaDateFolder, "stocuri.txt")
        private val comenziFile = File(bazaDateFolder, "comenzi.txt")

        ///TODO - modelare stoc depozit (baza de date cu stocurile de produse)
        ///DONE - stocul este modelat si persistat in stocuri.txt
        val stocProduseDefault: Map<String, Int> = mapOf(
            "Masca protectie" to 100,
            "Vaccin anti-COVID-19" to 20,
            "Combinezon" to 30,
            "Manusa chirurgicala" to 40
        )

        private fun initializareBazaDate() {
            if (!bazaDateFolder.exists()) {
                bazaDateFolder.mkdirs()
            }

            if (!stocFile.exists()) {
                val continutImplicit = stocProduseDefault.entries.joinToString(separator = "\n", postfix = "\n") {
                    "${it.key}|${it.value}"
                }
                stocFile.writeText(continutImplicit)
            }

            if (!comenziFile.exists()) {
                comenziFile.createNewFile()
            }
        }

        private fun citesteStocDinFisier(): MutableMap<String, Int> {
            initializareBazaDate()
            val stocCurent = mutableMapOf<String, Int>()

            stocFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { linie ->
                    val campuri = linie.split("|")
                    if (campuri.size == 2) {
                        val produs = campuri[0].trim()
                        val cantitate = campuri[1].trim().toIntOrNull() ?: 0
                        stocCurent[produs] = cantitate
                    }
                }

            return stocCurent
        }

        private fun scrieStocInFisier(stocCurent: Map<String, Int>) {
            val continut = stocCurent.entries.joinToString(separator = "\n", postfix = "\n") {
                "${it.key}|${it.value}"
            }
            stocFile.writeText(continut)
        }

        private fun citesteComandaDinFisier(identificatorComanda: Int): Pair<String, Int>? {
            initializareBazaDate()
            val prefixComanda = "$identificatorComanda|"
            val linieComanda = comenziFile.readLines().firstOrNull { it.startsWith(prefixComanda) } ?: return null
            val campuri = linieComanda.split("|")

            if (campuri.size < 3) {
                return null
            }

            val produsComandat = campuri[1].trim()
            val cantitateComandata = campuri[2].trim().toIntOrNull() ?: return null
            return produsComandat to cantitateComandata
        }
    }

    private fun acceptareComanda(identificator: Int, produs: String, cantitate: Int): String {
        println("Comanda cu identificatorul $identificator a fost acceptata!")
        return pregatireColet(produs, cantitate)
    }

    private fun respingereComanda(identificator: Int): String {
        println("Comanda cu identificatorul $identificator a fost respinsa! Stoc insuficient.")
        return "RESPINSA"
    }

    private fun verificareStoc(produs: String, cantitate: Int): Boolean {
        ///TODO - verificare stoc produs
        ///DONE - stocul produsului este verificat din fisierul stocuri.txt
        val stocCurent = citesteStocDinFisier()
        return (stocCurent[produs] ?: 0) >= cantitate
    }

    private fun pregatireColet(produs: String, cantitate: Int): String {
        ///TODO - retragere produs de pe stoc in cantitatea specificata
        ///DONE - cantitatea pregatita este scazuta din stoc si salvata in fisier
        val stocCurent = citesteStocDinFisier()
        val stocDisponibil = stocCurent[produs] ?: 0
        stocCurent[produs] = (stocDisponibil - cantitate).coerceAtLeast(0)
        scrieStocInFisier(stocCurent)

        println("Produsul $produs in cantitate de $cantitate buc. este pregatit de livrare.")
        return "$produs|$cantitate"
    }

    @Transformer(inputChannel = Processor.INPUT, outputChannel = Processor.OUTPUT)
    ///TODO - parametrul ar trebui sa fie doar numarul de inregistrare al comenzii si atat
    ///DONE - se primeste din flux doar ID-ul comenzii
    fun procesareComanda(comanda: String?): String {
        val identificatorComanda = comanda!!.trim().toInt()
        println("Procesez comanda cu identificatorul $identificatorComanda...")

        //TODO - procesare comanda in depozit
        //DONE - se citesc detaliile comenzii din fisier dupa ID si se proceseaza stocul
        val comandaInregistrata = citesteComandaDinFisier(identificatorComanda)
        val rezultatProcesareComanda: String = if (comandaInregistrata != null &&
            verificareStoc(comandaInregistrata.first, comandaInregistrata.second)
        ) {
            acceptareComanda(identificatorComanda, comandaInregistrata.first, comandaInregistrata.second)
        } else {
            respingereComanda(identificatorComanda)
        }
        println("Rezultat procesare depozit pentru comanda $identificatorComanda: $rezultatProcesareComanda")

        ///TODO - in loc sa se trimita mesajul cu toate datele in continuare, trebuie trimis doar ID-ul comenzii
        ///DONE - catre urmatorul microserviciu este propagat doar ID-ul comenzii
        return "$identificatorComanda"
    }
}

fun main(args: Array<String>) {
    runApplication<DepozitMicroservice>(*args)
}
