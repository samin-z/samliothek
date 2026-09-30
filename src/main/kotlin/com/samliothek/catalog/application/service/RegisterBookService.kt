package com.samliothek.catalog.application.service

import com.samliothek.catalog.application.port.`in`.RegisterBookCommand
import com.samliothek.catalog.application.port.`in`.RegisterBookUseCase
import com.samliothek.catalog.application.port.out.BookCatalog
import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ids.BookId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
internal class RegisterBookService(
    private val books: BookCatalog,
) : RegisterBookUseCase {
    @Transactional
    override fun register(command: RegisterBookCommand): Outcome<CatalogError, Book> {
        if (books.findByIsbn(command.isbn) != null) {
            return Failure(CatalogError.DuplicateIsbn(command.isbn))
        }
        val book =
            Book(
                id = BookId(UUID.randomUUID()),
                isbn = command.isbn,
                title = command.title.trim(),
                author = command.author.trim(),
                publishedYear = command.publishedYear,
            )
        books.save(book)
        return Success(book)
    }
}
