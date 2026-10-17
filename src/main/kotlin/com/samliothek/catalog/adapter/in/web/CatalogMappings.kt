package com.samliothek.catalog.adapter.`in`.web

import com.samliothek.catalog.adapter.`in`.web.dto.BookPageResponse
import com.samliothek.catalog.adapter.`in`.web.dto.BookResponse
import com.samliothek.catalog.adapter.`in`.web.dto.CopyResponse
import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.Copy

internal fun Book.toResponse() =
    BookResponse(
        id = id.value.toString(),
        isbn = isbn.value,
        title = title,
        author = author,
        publishedYear = publishedYear,
    )

internal fun List<Book>.toPageResponse(
    page: Int,
    size: Int,
) = BookPageResponse(
    items = map { it.toResponse() },
    page = page,
    size = size,
)

internal fun Copy.toResponse() =
    CopyResponse(
        id = id.value.toString(),
        bookId = bookId.value.toString(),
        barcode = barcode.value,
        status = status,
    )
