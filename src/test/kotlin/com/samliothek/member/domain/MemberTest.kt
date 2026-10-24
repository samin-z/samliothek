package com.samliothek.member.domain

import com.samliothek.shared.Email
import com.samliothek.shared.ids.MemberId
import com.samliothek.support.requireSuccess
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

/**
 * Member is a data holder — no behaviour like Copy.markOnLoan.
 * This test pins the shape from PROJECT.md §5.3 before production types exist.
 */
class MemberTest {
    @Test
    fun `member holds identity fields without embedding lending policy`() {
        val email = Email.of("sam@example.com").requireSuccess()
        val member =
            Member(
                id = MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333")),
                name = "Sam",
                email = email,
                tier = MembershipTier.STANDARD,
                joinedOn = LocalDate.of(2026, 1, 15),
            )

        member.tier shouldBe MembershipTier.STANDARD
        member.email.value shouldBe "sam@example.com"
        MembershipTier.entries.toSet() shouldBe
            setOf(MembershipTier.STANDARD, MembershipTier.PREMIUM)
    }

    @Test
    fun `member errors cover not found and duplicate email`() {
        val id = MemberId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
        val email = Email.of("sam@example.com").requireSuccess()

        val notFound: MemberError = MemberError.NotFound(id)
        val duplicate: MemberError = MemberError.DuplicateEmail(email)

        (notFound as MemberError.NotFound).id shouldBe id
        (duplicate as MemberError.DuplicateEmail).email shouldBe email
    }
}
