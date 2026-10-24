package com.samliothek.member.application.port.`in`

import com.samliothek.member.domain.Member
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ValidationError

@ConsistentCopyVisibility
data class ListMembersQuery private constructor(
    val page: Int,
    val size: Int,
) {
    companion object {
        const val DEFAULT_PAGE = 0
        const val DEFAULT_SIZE = 20
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 100

        fun of(
            page: Int = DEFAULT_PAGE,
            size: Int = DEFAULT_SIZE,
        ): Outcome<ValidationError, ListMembersQuery> {
            if (page < 0) {
                return Failure(ValidationError("page must be >= 0"))
            }
            if (size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
                return Failure(
                    ValidationError("size must be between $MIN_PAGE_SIZE and $MAX_PAGE_SIZE"),
                )
            }
            return Success(ListMembersQuery(page = page, size = size))
        }
    }
}

data class MemberPage(
    val items: List<Member>,
    val page: Int,
    val size: Int,
)

fun interface ListMembersUseCase {
    fun list(query: ListMembersQuery): MemberPage
}
