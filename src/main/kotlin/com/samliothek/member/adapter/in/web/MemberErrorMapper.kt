package com.samliothek.member.adapter.`in`.web

import com.samliothek.member.domain.MemberError
import com.samliothek.shared.problem.Problems
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity

internal fun interface MemberErrorMapper {
    fun map(
        error: MemberError,
        instance: String,
    ): ResponseEntity<ProblemDetail>
}

internal class MemberErrorProblemMapper : MemberErrorMapper {
    override fun map(
        error: MemberError,
        instance: String,
    ): ResponseEntity<ProblemDetail> =
        when (error) {
            is MemberError.NotFound ->
                problem(
                    HttpStatus.NOT_FOUND,
                    "member-not-found",
                    "Member not found",
                    "No member with id ${error.id.value}",
                    instance,
                )
            is MemberError.DuplicateEmail ->
                problem(
                    HttpStatus.CONFLICT,
                    "duplicate-email",
                    "Duplicate email",
                    "Email ${error.email.value} already registered",
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
