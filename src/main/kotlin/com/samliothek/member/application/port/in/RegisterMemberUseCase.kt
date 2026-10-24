package com.samliothek.member.application.port.`in`

import com.samliothek.member.domain.Member
import com.samliothek.member.domain.MemberError
import com.samliothek.member.domain.MembershipTier
import com.samliothek.shared.Email
import com.samliothek.shared.Outcome

data class RegisterMemberCommand(
    val name: String,
    val email: Email,
    val tier: MembershipTier,
)

fun interface RegisterMemberUseCase {
    fun register(command: RegisterMemberCommand): Outcome<MemberError, Member>
}
