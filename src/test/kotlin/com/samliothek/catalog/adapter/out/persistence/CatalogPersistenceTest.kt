package com.samliothek.catalog.adapter.out.persistence

import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.Copy
import com.samliothek.catalog.domain.CopyStatus
import com.samliothek.shared.Barcode
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId
import com.samliothek.support.PostgresIntegrationTest
import com.samliothek.support.requireIsbn
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/**
 * Persistence adapter tests against real Postgres (Testcontainers).
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

        books.search("testing", page = 0, size = 20).shouldHaveSize(1)
        books.search("newly", page = 0, size = 20).shouldHaveSize(1)
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

    @Test
    fun `finders return empty results when nothing matches`() {
        val missingBookId = BookId(UUID.randomUUID())

        books.findById(missingBookId).shouldBeNull()
        books.findByIsbn(isbn("9780000000002")).shouldBeNull()
        books.search("does-not-exist", page = 0, size = 20).shouldBeEmpty()
        copies.findByBarcode(Barcode("MISSING")).shouldBeNull()
        copies.findByBookId(missingBookId).shouldBeEmpty()
    }

    @Test
    fun `saving a duplicate isbn fails`() {
        val first =
            Book(
                id = BookId(UUID.randomUUID()),
                isbn = isbn("9780132350884"),
                title = "Testing Book",
                author = "Sample writer",
                publishedYear = 2008,
            )
        books.save(first)

        val duplicate =
            Book(
                id = BookId(UUID.randomUUID()),
                isbn = first.isbn,
                title = "Other title",
                author = "Other author",
                publishedYear = 2010,
            )

        assertThrows<DataIntegrityViolationException> {
            books.save(duplicate)
        }
    }

    @Test
    fun `saving a duplicate barcode fails`() {
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
        copies.save(
            Copy(
                id = CopyId(UUID.randomUUID()),
                bookId = bookId,
                barcode = Barcode("BIB-000123"),
                status = CopyStatus.AVAILABLE,
            ),
        )

        assertThrows<DataIntegrityViolationException> {
            copies.save(
                Copy(
                    id = CopyId(UUID.randomUUID()),
                    bookId = bookId,
                    barcode = Barcode("BIB-000123"),
                    status = CopyStatus.AVAILABLE,
                ),
            )
        }
    }

    @Test
    fun `search respects page size and offset`() {
        books.save(book("9780132350884", "Alpha Book", "Author A", 2001))
        books.save(book("9780201633610", "Beta Book", "Author B", 2002))
        books.save(book("9780596007126", "Gamma Book", "Author C", 2003))

        val firstPage = books.search("", page = 0, size = 2)
        val secondPage = books.search("", page = 1, size = 2)

        firstPage.shouldHaveSize(2)
        firstPage.map { it.title } shouldBe listOf("Alpha Book", "Beta Book")
        secondPage.shouldHaveSize(1)
        secondPage.single().title shouldBe "Gamma Book"
    }

    private fun book(
        isbnRaw: String,
        title: String,
        author: String,
        year: Int,
    ) = Book(
        id = BookId(UUID.randomUUID()),
        isbn = isbn(isbnRaw),
        title = title,
        author = author,
        publishedYear = year,
    )

    private fun isbn(raw: String) = requireIsbn(raw)
}
