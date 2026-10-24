package com.samliothek.member.application.service

import com.samliothek.member.application.port.`in`.RegisterMemberCommand
import com.samliothek.member.application.port.`in`.RegisterMemberUseCase
import com.samliothek.member.application.port.out.MemberDirectory
import com.samliothek.member.domain.Member
import com.samliothek.member.domain.MemberError
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ids.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
internal class RegisterMemberService(
    private val members: MemberDirectory,
    private val clock: Clock,
) : RegisterMemberUseCase {
    @Transactional
    override fun register(command: RegisterMemberCommand): Outcome<MemberError, Member> {
        if (members.findByEmail(command.email) != null) {
            return Failure(MemberError.DuplicateEmail(command.email))
        }
        val member =
            Member(
                id = MemberId(UUID.randomUUID()),
                name = command.name.trim(),
                email = command.email,
                tier = command.tier,
                joinedOn = LocalDate.now(clock),
            )
        members.save(member)
        return Success(member)
    }
}
