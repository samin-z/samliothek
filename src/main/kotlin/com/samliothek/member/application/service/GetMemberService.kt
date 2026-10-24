package com.samliothek.member.application.service

import com.samliothek.member.application.port.`in`.GetMemberUseCase
import com.samliothek.member.application.port.out.MemberDirectory
import com.samliothek.member.domain.Member
import com.samliothek.member.domain.MemberError
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ids.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class GetMemberService(
    private val members: MemberDirectory,
) : GetMemberUseCase {
    @Transactional(readOnly = true)
    override fun get(memberId: MemberId): Outcome<MemberError, Member> {
        val member = members.findById(memberId) ?: return Failure(MemberError.NotFound(memberId))
        return Success(member)
    }
}
