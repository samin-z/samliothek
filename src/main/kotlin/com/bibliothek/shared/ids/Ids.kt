package com.bibliothek.shared.ids

import java.util.UUID

@JvmInline
value class BookId(
    val value: UUID,
)

@JvmInline
value class CopyId(
    val value: UUID,
)

@JvmInline
value class MemberId(
    val value: UUID,
)

@JvmInline
value class LoanId(
    val value: UUID,
)
