package com.samliothek.member.adapter.`in`.web

import com.samliothek.member.adapter.`in`.web.dto.RegisterMemberRequest
import com.samliothek.member.application.port.`in`.GetMemberUseCase
import com.samliothek.member.application.port.`in`.ListMembersQuery
import com.samliothek.member.application.port.`in`.ListMembersUseCase
import com.samliothek.member.application.port.`in`.RegisterMemberCommand
import com.samliothek.member.application.port.`in`.RegisterMemberUseCase
import com.samliothek.shared.Email
import com.samliothek.shared.Outcome
import com.samliothek.shared.fold
import com.samliothek.shared.ids.MemberId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/members")
internal class MemberController(
    private val registerMember: RegisterMemberUseCase,
    private val getMember: GetMemberUseCase,
    private val listMembers: ListMembersUseCase,
    private val advice: MemberExceptionAdvice,
) {
    @PostMapping
    fun register(
        @Valid @RequestBody request: RegisterMemberRequest,
    ): ResponseEntity<*> {
        val email =
            when (val parsed = Email.of(request.email)) {
                is Outcome.Failure -> return advice.validationProblem(parsed.error)
                is Outcome.Success -> parsed.value
            }
        return registerMember
            .register(
                RegisterMemberCommand(
                    name = request.name,
                    email = email,
                    tier = request.tier,
                ),
            ).fold(
                onFailure = { advice.toProblem(it) },
                onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it.toResponse()) },
            )
    }

    @GetMapping("/{memberId}")
    fun get(
        @PathVariable memberId: UUID,
    ): ResponseEntity<*> =
        getMember.get(MemberId(memberId)).fold(
            onFailure = { advice.toProblem(it) },
            onSuccess = { ResponseEntity.ok(it.toResponse()) },
        )

    @GetMapping
    fun list(
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<*> {
        val query =
            when (val parsed = ListMembersQuery.of(page = page, size = size)) {
                is Outcome.Failure -> return advice.validationProblem(parsed.error)
                is Outcome.Success -> parsed.value
            }
        val result = listMembers.list(query)
        return ResponseEntity.ok(result.items.toPageResponse(result.page, result.size))
    }
}
