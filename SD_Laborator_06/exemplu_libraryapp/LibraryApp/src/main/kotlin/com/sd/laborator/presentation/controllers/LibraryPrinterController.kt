package com.sd.laborator.presentation.controllers

import com.sd.laborator.business.interfaces.ILibraryDAOService
import com.sd.laborator.business.interfaces.ILibraryPrinterService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class LibraryPrinterController {
    @Autowired
    private lateinit var _libraryDAOService: ILibraryDAOService

    @Autowired
    private lateinit var _libraryPrinterService: ILibraryPrinterService


    @RequestMapping("/print", method = [RequestMethod.GET])
    @ResponseBody
    fun customPrint(@RequestParam(required = true, name = "format", defaultValue = "") format: String): String {
        return when (format) {
            "html" -> _libraryPrinterService.printHTML(_libraryDAOService.getBooks())
            "json" -> _libraryPrinterService.printJSON(_libraryDAOService.getBooks())
            "raw" -> _libraryPrinterService.printRaw(_libraryDAOService.getBooks())
            else -> "Not implemented"
        }
    }

    @RequestMapping("/find", method = [RequestMethod.GET])
    @ResponseBody
    fun customFind(
        @RequestParam(required = false, name = "author", defaultValue = "") author: String,
        @RequestParam(required = false, name = "title", defaultValue = "") title: String,
        @RequestParam(required = false, name = "publisher", defaultValue = "") publisher: String
    ): String {
        if (author != "")
            return this._libraryPrinterService.printJSON(this._libraryDAOService.findAllByAuthor(author))
        if (title != "")
            return this._libraryPrinterService.printJSON(this._libraryDAOService.findAllByTitle(title))
        if (publisher != "")
            return this._libraryPrinterService.printJSON(this._libraryDAOService.findAllByPublisher(publisher))
        return "Not a valid field"
    }

    @RequestMapping("/find-and-print", method = [RequestMethod.GET])
    @ResponseBody
    fun findAndPrint(
        @RequestParam(required = false, name = "author", defaultValue = "") author: String,
        @RequestParam(required = false, name = "title", defaultValue = "") title: String,
        @RequestParam(required = false, name = "publisher", defaultValue = "") publisher: String,
        @RequestParam(required = false, name = "format", defaultValue = "json") format: String
    ): String {
        // Query string pt cache
        val query = "author=$author&title=$title&publisher=$publisher&format=$format"
        
        // Căutare în Cache via HTTP
        try {
            val cacheUrl = java.net.URL("http://localhost:8081/get-cache?query=${java.net.URLEncoder.encode(query, "UTF-8")}")
            val connection = cacheUrl.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                if (response.startsWith("HIT:")) {
                    return response.substring(4) // Return cached JSON/HTML
                }
            }
        } catch (e: Exception) {
            println("Cache /get error: ${e.message}")
        }

        // MISS -> Căutare manuală
        val books = when {
            author != ""    -> this._libraryDAOService.findAllByAuthor(author)
            title != ""     -> this._libraryDAOService.findAllByTitle(title)
            publisher != "" -> this._libraryDAOService.findAllByPublisher(publisher)
            else            -> this._libraryDAOService.getBooks()
        }

        // Afișare în formatul dorit
        val result = when (format) {
            "html" -> this._libraryPrinterService.printHTML(books)
            "raw"  -> this._libraryPrinterService.printRaw(books)
            else   -> this._libraryPrinterService.printJSON(books)
        }

        // Salvare in Cache
        try {
            val cachePostUrl = java.net.URL("http://localhost:8081/set-cache?query=${java.net.URLEncoder.encode(query, "UTF-8")}")
            val conn = cachePostUrl.openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "text/plain; charset=utf-8")
            conn.outputStream.write(result.toByteArray(Charsets.UTF_8))
            conn.outputStream.close()
            
            // Read response
            conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            println("Cache /set error: ${e.message}")
        }

        return result
    }

}