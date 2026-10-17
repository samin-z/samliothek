package com.samliothek.catalog.application.port.`in`

import com.samliothek.catalog.domain.Book
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ValidationError

@ConsistentCopyVisibility
data class SearchBooksQuery private constructor(
    val query: String,
    val page: Int,
    val size: Int,
) {
    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 100
        const val MAX_QUERY_LENGTH = 200

        fun of(
            query: String = "",
            page: Int = DEFAULT_PAGE,
            size: Int = DEFAULT_SIZE,
        ): Outcome<ValidationError, SearchBooksQuery> {
            val trimmed = query.trim()
            if (trimmed.length > MAX_QUERY_LENGTH) {
                return Failure(ValidationError("query must be at most $MAX_QUERY_LENGTH characters"))
            }
            if (page < 0) {
                return Failure(ValidationError("page must be >= 0"))
            }
            if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
                return Failure(
                    ValidationError("size must be between $MIN_PAGE_SIZE and $MAX_PAGE_SIZE"),
                )
            }
            return Success(SearchBooksQuery(query = trimmed, page = page, size = size))
        }
    }
}

data class BookPage(
    val items: List<Book>,
    val page: Int,
    val size: Int,
)

fun interface SearchBooksUseCase {
    fun search(query: SearchBooksQuery): BookPage
}
