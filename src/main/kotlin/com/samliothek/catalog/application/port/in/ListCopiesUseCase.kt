package com.samliothek.catalog.application.port.`in`

import com.samliothek.catalog.domain.CatalogError
import com.samliothek.catalog.domain.Copy
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId

fun interface ListCopiesUseCase {
    fun list(bookId: BookId): Outcome<CatalogError, List<Copy>>
}
