package com.samliothek.catalog.adapter.out.persistence

import com.samliothek.catalog.application.port.out.CopyCatalog
import com.samliothek.catalog.domain.Copy
import com.samliothek.shared.Barcode
import com.samliothek.shared.ids.BookId
import org.springframework.stereotype.Component

@Component
internal class CopyCatalogJdbcAdapter(
    private val copies: CopyJdbcRepository,
) : CopyCatalog {
    override fun save(copy: Copy) {
        copies.save(copy.toRecord())
    }

    override fun findByBookId(bookId: BookId): List<Copy> = copies.findByBookId(bookId.value).map { it.toDomain() }

    override fun findByBarcode(barcode: Barcode): Copy? = copies.findByBarcode(barcode.value)?.toDomain()
}
