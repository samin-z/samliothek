package com.samliothek.catalog.application.port.out

import com.samliothek.catalog.domain.Book
import com.samliothek.shared.Isbn
import com.samliothek.shared.ids.BookId

interface BookCatalog {
    fun findById(id: BookId): Book?

    fun findByIsbn(isbn: Isbn): Book?

    fun search(
        query: String,
        page: Int,
        size: Int,
    ): List<Book>

    fun save(book: Book)
}
