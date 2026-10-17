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
        val size = query.size.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val page = query.page.coerceAtLeast(0)
        val items = books.search(query.query, page, size)
        return BookPage(items = items, page = page, size = size)
    }

    private companion object {
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 100
    }
}
