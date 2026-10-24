package com.samliothek.member.adapter.`in`.web

import com.samliothek.member.domain.MemberError
import com.samliothek.shared.ValidationError
import com.samliothek.shared.problem.Problems
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@RestControllerAdvice(basePackages = ["com.samliothek.member.adapter.in.web"])
internal class MemberExceptionAdvice {
    private val memberErrors: MemberErrorMapper = MemberErrorProblemMapper()

    fun toProblem(
        error: MemberError,
        instance: String = currentInstance(),
    ): ResponseEntity<ProblemDetail> = memberErrors.map(error, instance)

    fun validationProblem(
        error: ValidationError,
        instance: String = currentInstance(),
    ): ResponseEntity<ProblemDetail> =
        ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(
                Problems.of(
                    status = HttpStatus.BAD_REQUEST.value(),
                    typeSlug = "validation",
                    title = "Validation failed",
                    detail = error.message,
                    instance = instance,
                ),
            )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val detail =
            ex.bindingResult.fieldErrors
                .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
                .ifBlank { "Request validation failed" }
        return validationProblem(ValidationError(detail))
    }

    private fun currentInstance(): String = ServletUriComponentsBuilder.fromCurrentRequest().build().path ?: "/"
}
