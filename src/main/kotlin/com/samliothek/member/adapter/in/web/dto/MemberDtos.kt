package com.samliothek.member.adapter.`in`.web.dto

import com.samliothek.member.domain.MembershipTier
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

internal data class RegisterMemberRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val email: String,
    @field:NotNull val tier: MembershipTier,
)

internal data class MemberResponse(
    val id: String,
    val name: String,
    val email: String,
    val tier: MembershipTier,
    val joinedOn: String,
)

internal data class MemberPageResponse(
    val items: List<MemberResponse>,
    val page: Int,
    val size: Int,
)
