package com.samliothek.member.application.service

import com.samliothek.member.application.port.`in`.ListMembersQuery
import com.samliothek.member.application.port.`in`.ListMembersUseCase
import com.samliothek.member.application.port.`in`.MemberPage
import com.samliothek.member.application.port.out.MemberDirectory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
internal class ListMembersService(
    private val members: MemberDirectory,
) : ListMembersUseCase {
    @Transactional(readOnly = true)
    override fun list(query: ListMembersQuery): MemberPage {
        val items = members.list(query.page, query.size)
        return MemberPage(items = items, page = query.page, size = query.size)
    }
}
