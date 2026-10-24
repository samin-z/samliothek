package com.samliothek.member.application

import com.samliothek.member.application.port.`in`.GetMemberUseCase
import com.samliothek.member.application.port.`in`.ListMembersQuery
import com.samliothek.member.application.port.`in`.ListMembersUseCase
import com.samliothek.member.application.port.`in`.RegisterMemberCommand
import com.samliothek.member.application.port.`in`.RegisterMemberUseCase
import com.samliothek.member.application.port.out.MemberDirectory
import com.samliothek.member.application.service.GetMemberService
import com.samliothek.member.application.service.ListMembersService
import com.samliothek.member.application.service.RegisterMemberService
import com.samliothek.member.domain.Member
import com.samliothek.member.domain.MemberError
import com.samliothek.member.domain.MembershipTier
import com.samliothek.shared.Email
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.MemberId
import com.samliothek.support.requireSuccess
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Use-case tests with an in-memory fake (no Spring, no database).
 */
class MemberUseCaseTest {
    private val clock: Clock =
        Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC)

    private lateinit var members: InMemoryMemberDirectory
    private lateinit var registerMember: RegisterMemberUseCase
    private lateinit var getMember: GetMemberUseCase
    private lateinit var listMembers: ListMembersUseCase

    @BeforeEach
    fun setUp() {
        members = InMemoryMemberDirectory()
        registerMember = RegisterMemberService(members, clock)
        getMember = GetMemberService(members)
        listMembers = ListMembersService(members)
    }

    @Test
    fun `register member stores a new record with joinedOn from the clock`() {
        val result = registerMember.register(samCommand())

        result.shouldBeInstanceOf<Outcome.Success<Member>>()
        val member = result.value
        member.name shouldBe "Sam"
        member.email.value shouldBe "sam@example.com"
        member.tier shouldBe MembershipTier.STANDARD
        member.joinedOn shouldBe LocalDate.of(2026, 1, 15)
        members.findById(member.id) shouldBe member
    }

    @Test
    fun `register member rejects a duplicate email`() {
        registerMember.register(samCommand())

        val result = registerMember.register(samCommand())

        result.shouldBeInstanceOf<Outcome.Failure<MemberError>>()
        result.error.shouldBeInstanceOf<MemberError.DuplicateEmail>()
    }

    @Test
    fun `get member returns not found for unknown id`() {
        val missing = MemberId(UUID.fromString("99999999-9999-9999-9999-999999999999"))

        val result = getMember.get(missing)

        result.shouldBeInstanceOf<Outcome.Failure<MemberError>>()
        result.error shouldBe MemberError.NotFound(missing)
    }

    @Test
    fun `get member returns a registered member`() {
        val registered = registerMember.register(samCommand()).requireSuccess()

        val result = getMember.get(registered.id)

        result.shouldBeInstanceOf<Outcome.Success<Member>>()
        result.value shouldBe registered
    }

    @Test
    fun `list members paginates results sorted by name`() {
        registerMember.register(
            RegisterMemberCommand(
                name = "Charlie",
                email = email("charlie@example.com"),
                tier = MembershipTier.STANDARD,
            ),
        )
        registerMember.register(
            RegisterMemberCommand(
                name = "Alice",
                email = email("alice@example.com"),
                tier = MembershipTier.PREMIUM,
            ),
        )
        registerMember.register(
            RegisterMemberCommand(
                name = "Bob",
                email = email("bob@example.com"),
                tier = MembershipTier.STANDARD,
            ),
        )

        val firstPage = listMembers.list(listQuery(page = 0, size = 2))
        val secondPage = listMembers.list(listQuery(page = 1, size = 2))

        firstPage.page shouldBe 0
        firstPage.size shouldBe 2
        firstPage.items.shouldHaveSize(2)
        firstPage.items.map { it.name } shouldBe listOf("Alice", "Bob")

        secondPage.page shouldBe 1
        secondPage.size shouldBe 2
        secondPage.items.shouldHaveSize(1)
        secondPage.items.single().name shouldBe "Charlie"
    }

    private fun samCommand() =
        RegisterMemberCommand(
            name = "Sam",
            email = email("sam@example.com"),
            tier = MembershipTier.STANDARD,
        )

    private fun email(raw: String): Email = Email.of(raw).requireSuccess()

    private fun listQuery(
        page: Int = 0,
        size: Int = 20,
    ): ListMembersQuery = ListMembersQuery.of(page = page, size = size).requireSuccess()
}

private class InMemoryMemberDirectory : MemberDirectory {
    private val byId = ConcurrentHashMap<MemberId, Member>()
    private val byEmail = ConcurrentHashMap<String, MemberId>()

    override fun findById(id: MemberId): Member? = byId[id]

    override fun findByEmail(email: Email): Member? = byEmail[email.value]?.let { byId[it] }

    override fun list(
        page: Int,
        size: Int,
    ): List<Member> {
        val sorted = byId.values.sortedBy { it.name }
        val from = page * size
        if (from >= sorted.size) return emptyList()
        return sorted.subList(from, minOf(from + size, sorted.size))
    }

    override fun save(member: Member) {
        byId[member.id] = member
        byEmail[member.email.value] = member.id
    }
}
