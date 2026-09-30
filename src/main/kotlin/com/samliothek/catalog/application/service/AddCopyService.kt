package com.samliothek.catalog.application.service

import com.samliothek.catalog.application.port.`in`.AddCopyCommand
import com.samliothek.catalog.application.port.`in`.AddCopyUseCase
import com.samliothek.catalog.application.port.out.BookCatalog
import com.samliothek.catalog.application.port.out.CopyCatalog
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.catalog.domain.Copy
import com.samliothek.catalog.domain.CopyStatus
import com.samliothek.shared.Outcome
import com.samliothek.shared.Outcome.Failure
import com.samliothek.shared.Outcome.Success
import com.samliothek.shared.ids.CopyId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
internal class AddCopyService(
    private val books: BookCatalog,
    private val copies: CopyCatalog,
) : AddCopyUseCase {
    @Transactional
    override fun add(command: AddCopyCommand): Outcome<CatalogError, Copy> {
        books.findById(command.bookId) ?: return Failure(CatalogError.BookNotFound(command.bookId))
        val copy =
            Copy(
                id = CopyId(UUID.randomUUID()),
                bookId = command.bookId,
                barcode = command.barcode,
                status = CopyStatus.AVAILABLE,
            )
        copies.save(copy)
        return Success(copy)
    }
}
