package com.samliothek.shared

import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success

@JvmInline
value class Isbn private constructor(
    val value: String,
) {
    companion object {
        private val PATTERN = Regex("^\\d{13}$")

        fun of(raw: String): Outcome<ValidationError, Isbn> {
            val normalised = raw.replace("-", "").trim()
            return if (PATTERN.matches(normalised)) {
                Success(Isbn(normalised))
            } else {
                Failure(ValidationError("isbn must be 13 digits"))
            }
        }
    }
}

@JvmInline
value class Barcode(
    val value: String,
)

@JvmInline
value class Email private constructor(
    val value: String,
) {
    companion object {
        private val PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        fun of(raw: String): Outcome<ValidationError, Email> {
            val normalised = raw.trim()
            return if (PATTERN.matches(normalised)) {
                Success(Email(normalised))
            } else {
                Failure(ValidationError("email must be a valid address"))
            }
        }
    }
}

data class ValidationError(
    val message: String,
)
