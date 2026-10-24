package com.samliothek.member.domain

import com.samliothek.shared.Email
import com.samliothek.shared.ids.MemberId
import java.time.LocalDate

data class Member(
    val id: MemberId,
    val name: String,
    val email: Email,
    val tier: MembershipTier,
    val joinedOn: LocalDate,
)
