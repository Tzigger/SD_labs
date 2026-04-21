package com.sd.laborator.persistence.interfaces

import com.sd.laborator.persistence.entities.BookEntity

interface IBookRepository {
    // DDL
    fun createTable()

    // Create
    fun add(book: BookEntity)

    // Retrieve
    fun getAll(): MutableList<BookEntity?>
    fun findAllByAuthor(author: String): MutableList<BookEntity?>
    fun findAllByTitle(title: String): MutableList<BookEntity?>
    fun findAllByPublisher(publisher: String): MutableList<BookEntity?>
}
