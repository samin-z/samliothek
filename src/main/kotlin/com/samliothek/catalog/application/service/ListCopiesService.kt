package com.samliothek.catalog.application.service

import com.samliothek.catalog.application.port.`in`.ListCopiesUseCase
import com.samliothek.catalog.application.port.out.BookCatalog
import com.samliothek.catalog.application.port.out.CopyCatalog
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.catalog.domain.Copy
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ids.BookId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class ListCopiesService(
    private val books: BookCatalog,
    private val copies: CopyCatalog,
) : ListCopiesUseCase {
    @Transactional(readOnly = true)
    override fun list(bookId: BookId): Outcome<CatalogError, List<Copy>> {
        books.findById(bookId) ?: return Failure(CatalogError.BookNotFound(bookId))
        return Success(copies.findByBookId(bookId))
    }
}
