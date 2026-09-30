package com.samliothek.catalog.domain

import com.samliothek.shared.Barcode
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import java.util.UUID

class CopyTest {
    private val bookId = BookId(UUID.fromString("11111111-1111-1111-1111-111111111111"))
    private val copyId = CopyId(UUID.fromString("22222222-2222-2222-2222-222222222222"))

    private fun availableCopy() =
        Copy(
            id = copyId,
            bookId = bookId,
            barcode = Barcode("BIB-000123"),
            status = CopyStatus.AVAILABLE,
        )

    @Test
    fun `markOnLoan succeeds when copy is available`() {
        val result = availableCopy().markOnLoan()

        result.shouldBeInstanceOf<Outcome.Success<Copy>>()
        result.value.status shouldBe CopyStatus.ON_LOAN
    }

    @Test
    fun `markOnLoan fails when copy is already on loan`() {
        val onLoan = availableCopy().copy(status = CopyStatus.ON_LOAN)

        val result = onLoan.markOnLoan()

        result.shouldBeInstanceOf<Outcome.Failure<CatalogError>>()
        result.error shouldBe CatalogError.CopyNotAvailable(copyId)
    }

    @Test
    fun `markOnLoan fails when copy is withdrawn`() {
        val withdrawn = availableCopy().copy(status = CopyStatus.WITHDRAWN)

        val result = withdrawn.markOnLoan()

        result.shouldBeInstanceOf<Outcome.Failure<CatalogError>>()
        result.error shouldBe CatalogError.CopyNotAvailable(copyId)
    }

    @Test
    fun `markAvailable restores an on-loan copy`() {
        val onLoan = availableCopy().copy(status = CopyStatus.ON_LOAN)

        val restored = onLoan.markAvailable()

        restored.status shouldBe CopyStatus.AVAILABLE
    }
}
