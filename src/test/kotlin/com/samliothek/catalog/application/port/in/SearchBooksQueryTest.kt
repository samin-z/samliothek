package com.samliothek.catalog.application.port.`in`

import com.samliothek.shared.Outcome
import com.samliothek.shared.ValidationError
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SearchBooksQueryTest {
    @Test
    fun `accepts defaults`() {
        val result = SearchBooksQuery.of()

        result.shouldBeInstanceOf<Outcome.Success<SearchBooksQuery>>()
        result.value.page shouldBe 0
        result.value.size shouldBe 20
        result.value.query shouldBe ""
    }

    @Test
    fun `trims query`() {
        val result = SearchBooksQuery.of(query = "  clean  ")

        result.shouldBeInstanceOf<Outcome.Success<SearchBooksQuery>>()
        result.value.query shouldBe "clean"
    }

    @Test
    fun `rejects negative page`() {
        val result = SearchBooksQuery.of(page = -1)

        result.shouldBeInstanceOf<Outcome.Failure<ValidationError>>()
        result.error.message shouldBe "page must be >= 0"
    }

    @Test
    fun `rejects size below minimum`() {
        val result = SearchBooksQuery.of(size = 0)

        result.shouldBeInstanceOf<Outcome.Failure<ValidationError>>()
    }

    @Test
    fun `rejects size above maximum`() {
        val result = SearchBooksQuery.of(size = 101)

        result.shouldBeInstanceOf<Outcome.Failure<ValidationError>>()
    }

    @Test
    fun `rejects query that is too long`() {
        val result = SearchBooksQuery.of(query = "a".repeat(SearchBooksQuery.MAX_QUERY_LENGTH + 1))

        result.shouldBeInstanceOf<Outcome.Failure<ValidationError>>()
    }
}
