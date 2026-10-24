package com.samliothek.member.adapter.`in`.web

import com.samliothek.member.application.port.`in`.GetMemberUseCase
import com.samliothek.member.application.port.`in`.ListMembersUseCase
import com.samliothek.member.application.port.`in`.MemberPage
import com.samliothek.member.application.port.`in`.RegisterMemberUseCase
import com.samliothek.member.domain.Member
import com.samliothek.member.domain.MemberError
import com.samliothek.member.domain.MembershipTier
import com.samliothek.shared.Email
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.MemberId
import com.samliothek.support.requireSuccess
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.util.UUID

/**
 * HTTP adapter tests. Use cases are mocked — no database.
 */
@WebMvcTest(controllers = [MemberController::class])
@Import(MemberExceptionAdvice::class, MemberErrorProblemMapper::class)
class MemberControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var registerMember: RegisterMemberUseCase

    @MockitoBean
    private lateinit var getMember: GetMemberUseCase

    @MockitoBean
    private lateinit var listMembers: ListMembersUseCase

    private val memberId = MemberId(UUID.fromString("22b91111-1111-1111-1111-111111111111"))
    private val email = Email.of("sam@example.com").requireSuccess()

    @Test
    fun `POST members returns 201 with the created member`() {
        val member =
            Member(
                id = memberId,
                name = "Sam",
                email = email,
                tier = MembershipTier.STANDARD,
                joinedOn = LocalDate.of(2026, 1, 15),
            )
        given(registerMember.register(any())).willReturn(Outcome.Success(member))

        mockMvc
            .perform(
                post("/api/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Sam","email":"sam@example.com","tier":"STANDARD"}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(memberId.value.toString()))
            .andExpect(jsonPath("$.email").value("sam@example.com"))
            .andExpect(jsonPath("$.tier").value("STANDARD"))
            .andExpect(jsonPath("$.joinedOn").value("2026-01-15"))
    }

    @Test
    fun `POST members returns 400 for invalid email`() {
        mockMvc
            .perform(
                post("/api/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Sam","email":"not-an-email","tier":"STANDARD"}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/validation"))
    }

    @Test
    fun `POST members returns 409 for duplicate email`() {
        given(registerMember.register(any()))
            .willReturn(Outcome.Failure(MemberError.DuplicateEmail(email)))

        mockMvc
            .perform(
                post("/api/members")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"Sam","email":"sam@example.com","tier":"STANDARD"}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/duplicate-email"))
    }

    @Test
    fun `GET member returns 404 when missing`() {
        given(getMember.get(memberId)).willReturn(Outcome.Failure(MemberError.NotFound(memberId)))

        mockMvc
            .perform(get("/api/members/${memberId.value}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/member-not-found"))
    }

    @Test
    fun `GET members returns a page of members`() {
        val member =
            Member(
                id = memberId,
                name = "Sam",
                email = email,
                tier = MembershipTier.STANDARD,
                joinedOn = LocalDate.of(2026, 1, 15),
            )
        given(listMembers.list(any())).willReturn(MemberPage(items = listOf(member), page = 0, size = 20))

        mockMvc
            .perform(get("/api/members"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].name").value("Sam"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
    }

    @Test
    fun `GET members returns 400 for invalid page size`() {
        mockMvc
            .perform(get("/api/members").param("size", "0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/validation"))
    }
}
