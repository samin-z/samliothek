package com.samliothek.member.application.port.`in`

import com.samliothek.member.domain.Member
import com.samliothek.member.domain.MemberError
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.MemberId

fun interface GetMemberUseCase {
    fun get(memberId: MemberId): Outcome<MemberError, Member>
}
