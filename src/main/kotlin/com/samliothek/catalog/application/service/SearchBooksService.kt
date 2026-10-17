package com.samliothek.catalog.application.service

import com.samliothek.catalog.application.port.`in`.BookPage
import com.samliothek.catalog.application.port.`in`.SearchBooksQuery
import com.samliothek.catalog.application.port.`in`.SearchBooksUseCase
import com.samliothek.catalog.application.port.out.BookCatalog
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class SearchBooksService(
    private val books: BookCatalog,
) : SearchBooksUseCase {
    @Transactional(readOnly = true)
    override fun search(query: SearchBooksQuery): BookPage {
        val items = books.search(query.query, query.page, query.size)
        return BookPage(items = items, page = query.page, size = query.size)
    }
}
