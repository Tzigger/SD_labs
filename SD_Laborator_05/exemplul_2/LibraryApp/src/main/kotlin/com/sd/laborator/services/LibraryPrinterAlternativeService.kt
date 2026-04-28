package com.sd.laborator.services

import com.sd.laborator.interfaces.LibraryPrinter
import com.sd.laborator.model.Book
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class LibraryPrinterAlternativeService: LibraryPrinter {
    // Acest serviciu formateaza rezultatele in formatul cerut de client.
    override fun printHTML(books: Set<Book>): String {
        val builder = StringBuilder()
        builder.append("<html><head><title>Libraria mea HTML</title></head><body>")
        books.forEach {
            builder.append("<p><h3>")
                .append(escapeHtml(it.name))
                .append("</h3><h4>")
                .append(escapeHtml(it.author))
                .append("</h4><h5>")
                .append(escapeHtml(it.publisher))
                .append("</h5>")
                .append(escapeHtml(it.content))
                .append("</p><br/>")
        }
        builder.append("</body></html>")
        return builder.toString()
    }

    override fun printJSON(books: Set<Book>): String {
        val body = books.joinToString(",\n") {
            "    {\"Titlu\": \"${escapeJson(it.name)}\", \"Autor\":\"${escapeJson(it.author)}\", \"Editura\":\"${escapeJson(it.publisher)}\", \"Text\":\"${escapeJson(it.content)}\"}"
        }
        return "[\n$body\n]\n"
    }

    override fun printRaw(books: Set<Book>): String {
        val builder = StringBuilder()
        books.forEach {
            builder.append(it.name ?: "").append("\n")
                .append(it.author ?: "").append("\n")
                .append(it.publisher ?: "").append("\n")
                .append(it.content ?: "").append("\n\n")
        }
        return builder.toString()
    }

    override fun printXML(books: Set<Book>): String {
        val builder = StringBuilder("<library>\n")
        books.forEach {
            builder.append("  <book>\n")
                .append("    <title>").append(escapeXml(it.name)).append("</title>\n")
                .append("    <author>").append(escapeXml(it.author)).append("</author>\n")
                .append("    <publisher>").append(escapeXml(it.publisher)).append("</publisher>\n")
                .append("    <text>").append(escapeXml(it.content)).append("</text>\n")
                .append("  </book>\n")
        }
        builder.append("</library>\n")
        return builder.toString()
    }

    private fun escapeHtml(value: String?): String {
        return (value ?: "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }

    private fun escapeXml(value: String?): String {
        return (value ?: "")
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun escapeJson(value: String?): String {
        return (value ?: "")
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
