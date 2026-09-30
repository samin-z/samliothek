package com.samliothek.catalog.domain

import com.samliothek.shared.Barcode
import com.samliothek.shared.Isbn
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId

sealed interface CatalogError {
    data class BookNotFound(
        val id: BookId,
    ) : CatalogError

    data class DuplicateIsbn(
        val isbn: Isbn,
    ) : CatalogError

    data class CopyNotFound(
        val barcode: Barcode,
    ) : CatalogError

    data class CopyNotAvailable(
        val id: CopyId,
    ) : CatalogError
}
