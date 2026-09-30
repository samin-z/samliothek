package com.samliothek.catalog.adapter.out.persistence

import com.samliothek.catalog.application.port.out.BookCatalog
import com.samliothek.catalog.domain.Book
import com.samliothek.shared.Isbn
import com.samliothek.shared.ids.BookId
import org.springframework.stereotype.Component

@Component
class BookCatalogJdbcAdapter(
    private val books: BookJdbcRepository,
) : BookCatalog {
    override fun findById(id: BookId): Book? = books.findById(id.value).orElse(null)?.toDomain()

    override fun findByIsbn(isbn: Isbn): Book? = books.findByIsbn(isbn.value)?.toDomain()

    override fun search(
        query: String,
        page: Int,
        size: Int,
    ): List<Book> {
        val q = query.trim()
        val records =
            if (q.isEmpty()) {
                books.findPage(limit = size, offset = page * size)
            } else {
                books.searchByPattern(
                    pattern = "%$q%",
                    limit = size,
                    offset = page * size,
                )
            }
        return records.map { it.toDomain() }
    }

    override fun save(book: Book) {
        books.save(book.toRecord())
    }
}
