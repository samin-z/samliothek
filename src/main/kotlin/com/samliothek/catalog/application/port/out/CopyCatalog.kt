package com.samliothek.catalog.application.port.out

import com.samliothek.catalog.domain.Copy
import com.samliothek.shared.Barcode
import com.samliothek.shared.ids.BookId

interface CopyCatalog {
    fun save(copy: Copy)

    fun findByBookId(bookId: BookId): List<Copy>

    fun findByBarcode(barcode: Barcode): Copy?
}
