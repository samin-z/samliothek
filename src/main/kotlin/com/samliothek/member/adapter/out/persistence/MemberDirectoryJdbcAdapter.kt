package com.samliothek.member.adapter.out.persistence

import com.samliothek.member.application.port.out.MemberDirectory
import com.samliothek.member.domain.Member
import com.samliothek.shared.Email
import com.samliothek.shared.ids.MemberId
import org.springframework.stereotype.Component

@Component
internal class MemberDirectoryJdbcAdapter(
    private val members: MemberJdbcRepository,
) : MemberDirectory {
    override fun findById(id: MemberId): Member? = members.findById(id.value).orElse(null)?.toDomain()

    override fun findByEmail(email: Email): Member? = members.findByEmail(email.value)?.toDomain()

    override fun list(
        page: Int,
        size: Int,
    ): List<Member> = members.findPage(limit = size, offset = page * size).map { it.toDomain() }

    override fun save(member: Member) {
        members.save(member.toRecord())
    }
}
