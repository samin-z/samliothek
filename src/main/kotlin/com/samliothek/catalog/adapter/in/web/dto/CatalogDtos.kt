package com.samliothek.catalog.adapter.`in`.web.dto

import com.samliothek.catalog.domain.CopyStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

internal data class RegisterBookRequest(
    @field:NotBlank val isbn: String,
    @field:NotBlank val title: String,
    @field:NotBlank val author: String,
    @field:NotNull val publishedYear: Int,
)

internal data class BookResponse(
    val id: String,
    val isbn: String,
    val title: String,
    val author: String,
    val publishedYear: Int,
)

internal data class BookPageResponse(
    val items: List<BookResponse>,
    val page: Int,
    val size: Int,
)

internal data class AddCopyRequest(
    @field:NotBlank val barcode: String,
)

internal data class CopyResponse(
    val id: String,
    val bookId: String,
    val barcode: String,
    val status: CopyStatus,
)
