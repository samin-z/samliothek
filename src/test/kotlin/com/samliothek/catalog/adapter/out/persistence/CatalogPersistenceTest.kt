package com.samliothek.catalog.adapter.out.persistence

import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.Copy
import com.samliothek.catalog.domain.CopyStatus
import com.samliothek.shared.Barcode
import com.samliothek.shared.Isbn
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId
import com.samliothek.support.PostgresIntegrationTest
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/**
 * Persistence adapter tests against real Postgres (Testcontainers).
 * JDBC adapters are added in the next step to make these green.
 */
@DataJdbcTest(properties = ["spring.flyway.enabled=true"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(BookCatalogJdbcAdapter::class, CopyCatalogJdbcAdapter::class)
class CatalogPersistenceTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var books: BookCatalogJdbcAdapter

    @Autowired
    private lateinit var copies: CopyCatalogJdbcAdapter

    @Autowired
    private lateinit var jdbc: JdbcTemplate

    @Test
    fun `book round-trip preserves fields`() {
        val book =
            Book(
                id = BookId(UUID.randomUUID()),
                isbn = isbn("9780132350884"),
                title = "Testing Book",
                author = "Sample writer",
                publishedYear = 2008,
            )

        books.save(book)

        books.findById(book.id) shouldBe book
        books.findByIsbn(book.isbn) shouldBe book
    }

    @Test
    fun `search finds by title and author`() {
        books.save(
            Book(
                id = BookId(UUID.randomUUID()),
                isbn = isbn("9780132350884"),
                title = "Testing Book",
                author = "Sample writer",
                publishedYear = 2008,
            ),
        )
        books.save(
            Book(
                id = BookId(UUID.randomUUID()),
                isbn = isbn("9780201633610"),
                title = "Toasting Book",
                author = "Newly writer",
                publishedYear = 1994,
            ),
        )

        books.search("clean", page = 0, size = 20).shouldHaveSize(1)
        books.search("gamma", page = 0, size = 20).shouldHaveSize(1)
        books.search("", page = 0, size = 20).shouldHaveSize(2)
    }

    @Test
    fun `copy round-trip and find by book`() {
        val bookId = BookId(UUID.randomUUID())
        books.save(
            Book(
                id = bookId,
                isbn = isbn("9780132350884"),
                title = "Testing Book",
                author = "Sample writer",
                publishedYear = 2008,
            ),
        )
        val copy =
            Copy(
                id = CopyId(UUID.randomUUID()),
                bookId = bookId,
                barcode = Barcode("BIB-000123"),
                status = CopyStatus.AVAILABLE,
            )

        copies.save(copy)

        copies.findByBarcode(copy.barcode) shouldBe copy
        copies.findByBookId(bookId) shouldBe listOf(copy)
        jdbc.queryForObject("select count(*) from copies", Int::class.java) shouldBe 1
    }

    private fun isbn(raw: String): Isbn = (Isbn.of(raw) as Outcome.Success).value
}
