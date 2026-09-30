package com.samliothek.catalog.application.port.`in`

import com.samliothek.catalog.domain.CatalogError
import com.samliothek.catalog.domain.Copy
import com.samliothek.shared.Barcode
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId

data class AddCopyCommand(
    val bookId: BookId,
    val barcode: Barcode,
)

fun interface AddCopyUseCase {
    fun add(command: AddCopyCommand): Outcome<CatalogError, Copy>
}
