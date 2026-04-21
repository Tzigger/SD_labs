package com.sd.laborator.components

import com.sd.laborator.interfaces.LibraryDAO
import com.sd.laborator.interfaces.LibraryPrinter
import com.sd.laborator.model.Book
import com.sd.laborator.model.Content
import org.springframework.amqp.core.AmqpTemplate
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.Exception
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Component
class LibraryAppComponent {
    @Autowired
    private lateinit var libraryDAO: LibraryDAO

    @Autowired
    private lateinit var libraryPrinter: LibraryPrinter

    @Autowired
    private lateinit var connectionFactory: RabbitMqConnectionFactoryComponent
    private lateinit var amqpTemplate: AmqpTemplate

    @Autowired
    fun initTemplate() {
        this.amqpTemplate = connectionFactory.rabbitTemplate()
    }

    fun sendMessage(msg: String) {
        this.amqpTemplate.convertAndSend(connectionFactory.getExchange(),
                                         connectionFactory.getRoutingKey(),
                                         msg)
    }

    @RabbitListener(queues = ["\${libraryapp.rabbitmq.queue}"])
    fun recieveMessage(msg: String) {
        val processedMsg = decodeIncomingMessage(msg)
        try {
            val messageParts = processedMsg.split(":", limit = 2)
            val function = messageParts[0]
            val parameter = messageParts.getOrElse(1) { "" }
            val result: String? = when(function) {
                "print" -> customPrint(parameter)
                "find" -> customFind(parameter)
                "add" -> customAdd(parameter)
                else -> null
            }
            if (result != null) sendMessage(result)
        } catch (e: Exception) {
            println(e)
        }
    }

    fun customPrint(format: String): String {
        val parameters = parseParameters(format)
        val outputFormat = parameters["format"] ?: format
        return printByFormat(libraryDAO.getBooks(), outputFormat)
    }

    fun customFind(searchParameter: String): String {
        val parameters = parseParameters(searchParameter)
        val field = parameters["field"] ?: searchParameter.substringBefore("=")
        val value = parameters["value"] ?: searchParameter.substringAfter("=", "")
        val outputFormat = parameters["format"] ?: "json"
        val books = when(field) {
            "author" -> this.libraryDAO.findAllByAuthor(value)
            "title" -> this.libraryDAO.findAllByTitle(value)
            "publisher" -> this.libraryDAO.findAllByPublisher(value)
            else -> return "Not a valid field"
        }
        return printByFormat(books, outputFormat)
    }

    fun customAdd(payload: String): String {
        val parameters = parseParameters(payload)
        val author = parameters["author"]?.trim().orEmpty()
        val title = parameters["title"]?.trim().orEmpty()
        val publisher = parameters["publisher"]?.trim().orEmpty()
        val text = parameters["text"]?.trim().orEmpty()

        if (author.isEmpty() || title.isEmpty() || publisher.isEmpty() || text.isEmpty()) {
            return "Date invalide: toate campurile sunt obligatorii"
        }

        return if (addBook(Book(Content(author, text, title, publisher)))) {
            "Cartea a fost adaugata"
        } else {
            "Nu s-a putut adauga cartea"
        }
    }

    fun addBook(book: Book): Boolean {
        return try {
            this.libraryDAO.addBook(book)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun printByFormat(books: Set<Book>, format: String): String {
        return when(format.lowercase()) {
            "html" -> libraryPrinter.printHTML(books)
            "json" -> libraryPrinter.printJSON(books)
            "raw", "text", "txt" -> libraryPrinter.printRaw(books)
            "xml" -> libraryPrinter.printXML(books)
            else -> "Not implemented"
        }
    }

    private fun parseParameters(parameterString: String): Map<String, String> {
        if (!parameterString.contains("=")) {
            return emptyMap()
        }

        return parameterString.split("&")
            .mapNotNull { entry ->
                val keyValue = entry.split("=", limit = 2)
                if (keyValue.size != 2) {
                    null
                } else {
                    val key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8.name())
                    val value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8.name())
                    key to value
                }
            }
            .toMap()
    }

    private fun decodeIncomingMessage(msg: String): String {
        return try {
            val chunks = msg.split(",")
            val looksEncoded = chunks.size > 1 && chunks.all { it.trim().matches(Regex("\\d+")) }
            if (looksEncoded) {
                chunks.map { it.trim().toInt().toChar() }.joinToString(separator = "")
            } else {
                msg
            }
        } catch (_: Exception) {
            msg
        }
    }

}