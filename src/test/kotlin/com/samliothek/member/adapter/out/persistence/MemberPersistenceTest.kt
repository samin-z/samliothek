package com.samliothek.member.adapter.out.persistence

import com.samliothek.member.domain.Member
import com.samliothek.member.domain.MembershipTier
import com.samliothek.shared.Email
import com.samliothek.shared.ids.MemberId
import com.samliothek.support.PostgresIntegrationTest
import com.samliothek.support.requireSuccess
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.UUID

/**
 * Persistence adapter tests against real Postgres (Testcontainers).
 */
@DataJdbcTest(properties = ["spring.flyway.enabled=true"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MemberDirectoryJdbcAdapter::class)
class MemberPersistenceTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var members: MemberDirectoryJdbcAdapter

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun `member round-trip preserves fields`() {
        val member = member("Sam", "sam@example.com", MembershipTier.STANDARD)

        members.save(member)

        members.findById(member.id) shouldBe member
        members.findByEmail(member.email) shouldBe member
        jdbc.queryForObject("select count(*) from members", Int::class.java) shouldBe 1
    }

    @Test
    fun `finders return empty results when nothing matches`() {
        val missing = MemberId(UUID.randomUUID())

        members.findById(missing).shouldBeNull()
        members.findByEmail(email("missing@example.com")).shouldBeNull()
        members.list(page = 0, size = 20).shouldBeEmpty()
    }

    @Test
    fun `saving a duplicate email fails`() {
        val first = member("Sam", "sam@example.com", MembershipTier.STANDARD)
        members.save(first)

        assertThrows<DataIntegrityViolationException> {
            members.save(member("Other", "sam@example.com", MembershipTier.PREMIUM))
        }
    }

    @Test
    fun `list paginates members sorted by name`() {
        members.save(member("Charlie", "charlie@example.com", MembershipTier.STANDARD))
        members.save(member("Alice", "alice@example.com", MembershipTier.PREMIUM))
        members.save(member("Bob", "bob@example.com", MembershipTier.STANDARD))

        val firstPage = members.list(page = 0, size = 2)
        val secondPage = members.list(page = 1, size = 2)

        firstPage.shouldHaveSize(2)
        firstPage.map { it.name } shouldBe listOf("Alice", "Bob")
        secondPage.shouldHaveSize(1)
        secondPage.single().name shouldBe "Charlie"
    }

    private fun member(
        name: String,
        emailRaw: String,
        tier: MembershipTier,
    ) = Member(
        id = MemberId(UUID.randomUUID()),
        name = name,
        email = email(emailRaw),
        tier = tier,
        joinedOn = LocalDate.of(2026, 1, 15),
    )

    private fun email(raw: String): Email = Email.of(raw).requireSuccess()
}
