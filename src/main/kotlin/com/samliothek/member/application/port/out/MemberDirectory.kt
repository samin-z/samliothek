package com.samliothek.member.application.port.out

import com.samliothek.member.domain.Member
import com.samliothek.shared.Email
import com.samliothek.shared.ids.MemberId

interface MemberDirectory {
    fun findById(id: MemberId): Member?

    fun findByEmail(email: Email): Member?

    fun list(
        page: Int,
        size: Int,
    ): List<Member>

    fun save(member: Member)
}
