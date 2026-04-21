package com.sd.laborator.persistence.repositories

import com.sd.laborator.persistence.entities.BookEntity
import com.sd.laborator.persistence.interfaces.IBookRepository
import com.sd.laborator.persistence.mappers.BookRowMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository

@Repository
class BookRepository : IBookRepository {

    @Autowired
    private lateinit var _jdbcTemplate: JdbcTemplate

    private var _rowMapper: RowMapper<BookEntity?> = BookRowMapper()

    override fun createTable() {
        _jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS books (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                name      VARCHAR(200) UNIQUE,
                author    VARCHAR(100),
                publisher VARCHAR(100),
                content   TEXT
            )
        """.trimIndent())
    }

    override fun add(book: BookEntity) {
        _jdbcTemplate.update(
            "INSERT OR IGNORE INTO books (name, author, publisher, content) VALUES (?, ?, ?, ?)",
            book.name, book.author, book.publisher, book.content
        )
    }

    override fun getAll(): MutableList<BookEntity?> {
        return _jdbcTemplate.query("SELECT * FROM books", _rowMapper)
    }

    override fun findAllByAuthor(author: String): MutableList<BookEntity?> {
        return _jdbcTemplate.query(
            "SELECT * FROM books WHERE author = ?",
            _rowMapper, author
        )
    }

    override fun findAllByTitle(title: String): MutableList<BookEntity?> {
        return _jdbcTemplate.query(
            "SELECT * FROM books WHERE name = ?",
            _rowMapper, title
        )
    }

    override fun findAllByPublisher(publisher: String): MutableList<BookEntity?> {
        return _jdbcTemplate.query(
            "SELECT * FROM books WHERE publisher = ?",
            _rowMapper, publisher
        )
    }
}
