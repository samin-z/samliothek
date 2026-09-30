package com.samliothek.catalog.adapter.out.persistence

import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.Copy
import com.samliothek.catalog.domain.CopyStatus
import com.samliothek.shared.Barcode
import com.samliothek.shared.Isbn
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId
import java.time.Instant

fun Book.toRecord(now: Instant = Instant.now()): BookRecord =
    BookRecord(
        id = id.value,
        isbn = isbn.value,
        title = title,
        author = author,
        publishedYear = publishedYear,
        createdAt = now,
    )

fun BookRecord.toDomain(): Book =
    Book(
        id = BookId(id),
        isbn = (Isbn.of(isbn) as Outcome.Success).value,
        title = title,
        author = author,
        publishedYear = publishedYear,
    )

fun Copy.toRecord(now: Instant = Instant.now()): CopyRecord =
    CopyRecord(
        id = id.value,
        bookId = bookId.value,
        barcode = barcode.value,
        status = status.name,
        createdAt = now,
    )

fun CopyRecord.toDomain(): Copy =
    Copy(
        id = CopyId(id),
        bookId = BookId(bookId),
        barcode = Barcode(barcode),
        status = CopyStatus.valueOf(status),
    )
