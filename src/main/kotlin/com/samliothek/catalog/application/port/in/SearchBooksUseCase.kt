package com.samliothek.catalog.application.port.`in`

import com.samliothek.catalog.domain.Book

data class SearchBooksQuery(
    val query: String = "",
    val page: Int = 0,
    val size: Int = 20,
)

data class BookPage(
    val items: List<Book>,
    val page: Int,
    val size: Int,
)

fun interface SearchBooksUseCase {
    fun search(query: SearchBooksQuery): BookPage
}
