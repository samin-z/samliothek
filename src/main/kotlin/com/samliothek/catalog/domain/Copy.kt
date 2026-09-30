package com.samliothek.catalog.domain

import com.samliothek.shared.Barcode
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId

data class Copy(
    val id: CopyId,
    val bookId: BookId,
    val barcode: Barcode,
    val status: CopyStatus,
) {
    fun markOnLoan(): Outcome<CatalogError, Copy> =
        if (status == CopyStatus.AVAILABLE) {
            Success(copy(status = CopyStatus.ON_LOAN))
        } else {
            Failure(CatalogError.CopyNotAvailable(id))
        }

    fun markAvailable(): Copy = copy(status = CopyStatus.AVAILABLE)
}
