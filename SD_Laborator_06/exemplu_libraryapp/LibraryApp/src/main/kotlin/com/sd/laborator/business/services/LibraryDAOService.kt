package com.sd.laborator.business.services

import com.sd.laborator.business.interfaces.ILibraryDAOService
import com.sd.laborator.business.models.Book
import com.sd.laborator.business.models.Content
import com.sd.laborator.persistence.entities.BookEntity
import com.sd.laborator.persistence.interfaces.IBookRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class LibraryDAOService : ILibraryDAOService {

    @Autowired
    private lateinit var _bookRepository: IBookRepository

    // Convertim BookEntity (nivel persistență) → Book (nivel business)
    private fun entityToBook(entity: BookEntity): Book {
        return Book(Content(entity.author, entity.content, entity.name, entity.publisher))
    }

    @PostConstruct
    fun init() {
        // Creăm tabela dacă nu există
        _bookRepository.createTable()

        // Inserăm datele inițiale (INSERT OR IGNORE nu va duplica)
        val initialBooks = listOf(
            BookEntity(0, "Programming in LUA", "Roberto Ierusalimschy", "Teora",
                "Preface. When Waldemar, Luiz, and I started the development of Lua, back in 1993, we could hardly imagine that it would spread as it did. ..."),
            BookEntity(0, "Steaua Sudului", "Jules Verne", "Corint",
                "Nemaipomeniti sunt francezii astia! - Vorbiti, domnule, va ascult! ...."),
            BookEntity(0, "O calatorie spre centrul pamantului", "Jules Verne", "Polirom",
                "Cuvant Inainte. Imaginatia copiilor - zicea un mare poet romantic spaniol - este asemenea unui cal nazdravan, iar curiozitatea lor e pintenul ce-l fugareste prin lumea celor mai indraznete proiecte."),
            BookEntity(0, "Insula Misterioasa", "Jules Verne", "Teora",
                "Partea intai. Naufragiatii vazduhului. Capitolul 1. Uraganul din 1865. ..."),
            BookEntity(0, "Casa cu aburi", "Jules Verne", "Albatros",
                "Capitolul I. S-a pus un premiu pe capul unui om. Se ofera premiu de 2000 de lire ...")
        )
        initialBooks.forEach { _bookRepository.add(it) }
    }

    override fun createBookTable() {
        _bookRepository.createTable()
    }

    override fun getBooks(): Set<Book> {
        return _bookRepository.getAll()
            .filterNotNull()
            .map { entityToBook(it) }
            .toSet()
    }

    override fun addBook(book: Book) {
        val entity = BookEntity(
            0,
            book.name ?: "",
            book.author ?: "",
            book.publisher ?: "",
            book.content ?: ""
        )
        _bookRepository.add(entity)
    }

    override fun findAllByAuthor(author: String): Set<Book> {
        return _bookRepository.findAllByAuthor(author)
            .filterNotNull()
            .map { entityToBook(it) }
            .toSet()
    }

    override fun findAllByTitle(title: String): Set<Book> {
        return _bookRepository.findAllByTitle(title)
            .filterNotNull()
            .map { entityToBook(it) }
            .toSet()
    }

    override fun findAllByPublisher(publisher: String): Set<Book> {
        return _bookRepository.findAllByPublisher(publisher)
            .filterNotNull()
            .map { entityToBook(it) }
            .toSet()
    }
}