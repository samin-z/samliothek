package com.samliothek.catalog.adapter.`in`.web

import com.samliothek.catalog.domain.CatalogError
import com.samliothek.shared.ValidationError
import com.samliothek.shared.problem.Problems
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestControllerAdvice(basePackages = ["com.samliothek.catalog.adapter.in.web"])
class CatalogExceptionAdvice {
    fun toProblem(
        error: CatalogError,
        instance: String = currentInstance(),
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is CatalogError.BookNotFound ->
                problem(404, "book-not-found", "Book not found", "No book with id ${error.id.value}", instance)
            is CatalogError.DuplicateIsbn ->
                problem(409, "duplicate-isbn", "Duplicate ISBN", "ISBN ${error.isbn.value} already registered", instance)
            is CatalogError.CopyNotFound ->
                problem(404, "copy-not-found", "Copy not found", "No copy with barcode ${error.barcode.value}", instance)
            is CatalogError.CopyNotAvailable ->
                problem(409, "copy-not-available", "Copy not available", "Copy ${error.id.value} is not available", instance)
        }

    fun validationProblem(
        error: ValidationError,
        instance: String = currentInstance(),
    ): ResponseEntity<ProblemDetail> =
        problem(400, "validation", "Validation failed", error.message, instance)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val detail =
            ex.bindingResult.fieldErrors
                .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
                .ifBlank { "Request validation failed" }
        return validationProblem(ValidationError(detail))
    }

    private fun problem(
        status: Int,
        typeSlug: String,
        title: String,
        detail: String,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity
            .status(HttpStatus.valueOf(status))
            .body(Problems.of(status, typeSlug, title, detail, instance))

    private fun currentInstance(): String =
        ServletUriComponentsBuilder.fromCurrentRequest().build().path ?: "/"
}
