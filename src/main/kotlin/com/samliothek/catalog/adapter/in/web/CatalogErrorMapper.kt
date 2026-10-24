package com.samliothek.catalog.adapter.`in`.web

import com.samliothek.catalog.domain.CatalogError
import com.samliothek.shared.problem.Problems
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity

internal fun interface CatalogErrorMapper {
    fun map(
        error: CatalogError,
        instance: String,
    ): ResponseEntity<ProblemDetail>
}

internal class CatalogErrorProblemMapper : CatalogErrorMapper {
    override fun map(
        error: CatalogError,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is CatalogError.BookNotFound ->
                problem(
                    HttpStatus.NOT_FOUND,
                    "book-not-found",
                    "Book not found",
                    "No book with id ${error.id.value}",
                    instance,
                )
            is CatalogError.DuplicateIsbn ->
                problem(
                    HttpStatus.CONFLICT,
                    "duplicate-isbn",
                    "Duplicate ISBN",
                    "ISBN ${error.isbn.value} already registered",
                    instance,
                )
            is CatalogError.CopyNotFound ->
                problem(
                    HttpStatus.NOT_FOUND,
                    "copy-not-found",
                    "Copy not found",
                    "No copy with barcode ${error.barcode.value}",
                    instance,
                )
            is CatalogError.CopyNotAvailable ->
                problem(
                    HttpStatus.CONFLICT,
                    "copy-not-available",
                    "Copy not available",
                    "Copy ${error.id.value} is not available",
                    instance,
                )
        }

    private fun problem(
        status: HttpStatus,
        typeSlug: String,
        title: String,
        detail: String,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity
            .status(status)
            .body(Problems.of(status.value(), typeSlug, title, detail, instance))
}
