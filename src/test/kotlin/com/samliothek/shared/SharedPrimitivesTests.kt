package com.samliothek.shared

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class SharedPrimitivesTests {
    @Test
    fun `isbn accepts 13 digits and strips hyphens`() {
        val result = Isbn.of("978-0-13-235088-4")
        result.shouldBeInstanceOf<Outcome.Success<Isbn>>()
        result.value.value shouldBe "9780132350884"
    }

    @Test
    fun `isbn rejects non-13-digit input`() {
        val result = Isbn.of("not-an-isbn")
        result.shouldBeInstanceOf<Outcome.Failure<ValidationError>>()
    }

    @Test
    fun `email rejects missing at-sign`() {
        val result = Email.of("not-an-email")
        result.shouldBeInstanceOf<Outcome.Failure<ValidationError>>()
    }
}
