package com.samliothek.catalog.application.service

import com.samliothek.catalog.application.port.`in`.GetBookUseCase
import com.samliothek.catalog.application.port.out.BookCatalog
import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ids.BookId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetBookService(
    private val books: BookCatalog,
) : GetBookUseCase {
    @Transactional(readOnly = true)
    override fun get(bookId: BookId): Outcome<CatalogError, Book> {
        val book = books.findById(bookId) ?: return Failure(CatalogError.BookNotFound(bookId))
        return Success(book)
    }
}
