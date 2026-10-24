package com.samliothek.member.adapter.`in`.web

import com.samliothek.member.adapter.`in`.web.dto.MemberPageResponse
import com.samliothek.member.adapter.`in`.web.dto.MemberResponse
import com.samliothek.member.domain.Member

internal fun Member.toResponse() =
    MemberResponse(
        id = id.value.toString(),
        name = name,
        email = email.value,
        tier = tier,
        joinedOn = joinedOn.toString(),
    )

internal fun List<Member>.toPageResponse(
    page: Int,
    size: Int,
) = MemberPageResponse(
    items = map { it.toResponse() },
    page = page,
    size = size,
)
