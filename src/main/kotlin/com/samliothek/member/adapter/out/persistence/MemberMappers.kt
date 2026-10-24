package com.samliothek.member.adapter.out.persistence

import com.samliothek.member.domain.Member
import com.samliothek.shared.Email
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.MemberId

internal fun Member.toRecord(): MemberRecord =
    MemberRecord(
        id = id.value,
        name = name,
        email = email.value,
        tier = tier,
        joinedOn = joinedOn,
    )

internal fun MemberRecord.toDomain(): Member =
    Member(
        id = MemberId(id),
        name = name,
        email = requirePersistedEmail(email),
        tier = tier,
        joinedOn = joinedOn,
    )

private fun requirePersistedEmail(raw: String): Email =
    when (val parsed = Email.of(raw)) {
        is Outcome.Success -> parsed.value
        is Outcome.Failure -> error("Corrupt email in database: '$raw' (${parsed.error.message})")
    }
