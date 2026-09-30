package com.samliothek.catalog.application.port.`in`

import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId

fun interface GetBookUseCase {
    fun get(bookId: BookId): Outcome<CatalogError, Book>
}
