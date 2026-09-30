package com.samliothek.catalog.application.port.`in`

import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.shared.Isbn
import com.samliothek.shared.Outcome

data class RegisterBookCommand(
    val isbn: Isbn,
    val title: String,
    val author: String,
    val publishedYear: Int,
)

fun interface RegisterBookUseCase {
    fun register(command: RegisterBookCommand): Outcome<CatalogError, Book>
}
