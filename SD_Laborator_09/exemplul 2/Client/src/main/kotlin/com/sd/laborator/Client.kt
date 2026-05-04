package com.sd.laborator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.stream.annotation.EnableBinding
import org.springframework.cloud.stream.messaging.Source
import org.springframework.context.annotation.Bean
import org.springframework.integration.annotation.InboundChannelAdapter
import org.springframework.integration.annotation.Poller
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import java.io.File
import java.util.UUID
import kotlin.random.Random

@EnableBinding(Source::class)
@SpringBootApplication
class ClientMicroservice {
    companion object {
        private val bazaDateFolder = File(System.getProperty("java.io.tmpdir"), "sd_lab09_db")
        private val produseFile = File(bazaDateFolder, "produse.txt")
        private val clientiFile = File(bazaDateFolder, "clienti.txt")

        private val produseDefault: List<String> = arrayListOf(
            "Masca protectie",
            "Vaccin anti-COVID-19",
            "Combinezon",
            "Manusa chirurgicala"
        )

        ///TODO - lista de produse sa fie preluata din baza de date / din fisier
        ///DONE - lista de produse este citita din fisierul text produse.txt
        private fun citesteListaProduseDinFisier(): List<String> {
            initializareBazaDate()
            val produseDinFisier = produseFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            return if (produseDinFisier.isEmpty()) produseDefault else produseDinFisier
        }

        private fun initializareBazaDate() {
            if (!bazaDateFolder.exists()) {
                bazaDateFolder.mkdirs()
            }

            if (!produseFile.exists()) {
                produseFile.writeText(produseDefault.joinToString(separator = "\n", postfix = "\n"))
            }

            if (!clientiFile.exists()) {
                clientiFile.createNewFile()
            }
        }

        private fun inregistreazaClient(identitateClient: String, adresaLivrare: String) {
            initializareBazaDate()
            val clientId = UUID.randomUUID().toString()
            clientiFile.appendText("$clientId|$identitateClient|$adresaLivrare\n")
            println("Client inregistrat in baza de date text cu id $clientId.")
        }
    }

    @Bean
    @InboundChannelAdapter(value = Source.OUTPUT, poller = [Poller(fixedDelay = "10000", maxMessagesPerPoll = "1")])
    fun comandaProdus(): () -> Message<String> {
        return {
            val listaProduse = citesteListaProduseDinFisier()
            val produsComandat = listaProduse[(0 until listaProduse.size).shuffled()[0]]
            val cantitate: Int = Random.nextInt(1, 100)
            val identitateClient = "Popescu Ion ${Random.nextInt(1, 1000)}"
            val adresaLivrare = "Codrii Vlasiei nr ${Random.nextInt(1, 200)}"

            ///TODO - datele clientului sa fie citite dinamic si inregistrate in baza de date
            ///DONE - datele clientului sunt generate dinamic si persistate in clienti.txt
            inregistreazaClient(identitateClient, adresaLivrare)

            val mesaj = "$identitateClient|$produsComandat|$cantitate|$adresaLivrare"
            MessageBuilder.withPayload(mesaj).build()
        }
    }
}

fun main(args: Array<String>) {
    runApplication<ClientMicroservice>(*args)
}
