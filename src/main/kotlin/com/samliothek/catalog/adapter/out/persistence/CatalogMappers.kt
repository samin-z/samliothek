package com.samliothek.catalog.adapter.out.persistence

import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.Copy
import com.samliothek.shared.Barcode
import com.samliothek.shared.Isbn
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId

internal fun Book.toRecord(): BookRecord =
    BookRecord(
        id = id.value,
        isbn = isbn.value,
        title = title,
        author = author,
        publishedYear = publishedYear,
    )

internal fun BookRecord.toDomain(): Book =
    Book(
        id = BookId(id),
        isbn = requirePersistedIsbn(isbn),
        title = title,
        author = author,
        publishedYear = publishedYear,
    )

internal fun Copy.toRecord(): CopyRecord =
    CopyRecord(
        id = id.value,
        bookId = bookId.value,
        barcode = barcode.value,
        status = status,
    )

internal fun CopyRecord.toDomain(): Copy =
    Copy(
        id = CopyId(id),
        bookId = BookId(bookId),
        barcode = Barcode(barcode),
        status = status,
    )

private fun requirePersistedIsbn(raw: String): Isbn =
    when (val parsed = Isbn.of(raw)) {
        is Outcome.Success -> parsed.value
        is Outcome.Failure -> error("Corrupt ISBN in database: '$raw' (${parsed.error.message})")
    }
