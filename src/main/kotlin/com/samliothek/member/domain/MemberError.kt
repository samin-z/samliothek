package com.samliothek.member.domain

import com.samliothek.shared.Email
import com.samliothek.shared.ids.MemberId

sealed interface MemberError {
    data class NotFound(
        val id: MemberId,
    ) : MemberError

    data class DuplicateEmail(
        val email: Email,
    ) : MemberError
}
