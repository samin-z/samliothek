package com.samliothek.catalog.application

import com.samliothek.catalog.application.port.`in`.AddCopyCommand
import com.samliothek.catalog.application.port.`in`.AddCopyUseCase
import com.samliothek.catalog.application.port.`in`.GetBookUseCase
import com.samliothek.catalog.application.port.`in`.ListCopiesUseCase
import com.samliothek.catalog.application.port.`in`.RegisterBookCommand
import com.samliothek.catalog.application.port.`in`.RegisterBookUseCase
import com.samliothek.catalog.application.port.`in`.SearchBooksQuery
import com.samliothek.catalog.application.port.`in`.SearchBooksUseCase
import com.samliothek.catalog.application.port.out.BookCatalog
import com.samliothek.catalog.application.port.out.CopyCatalog
import com.samliothek.catalog.application.service.AddCopyService
import com.samliothek.catalog.application.service.GetBookService
import com.samliothek.catalog.application.service.ListCopiesService
import com.samliothek.catalog.application.service.RegisterBookService
import com.samliothek.catalog.application.service.SearchBooksService
import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.catalog.domain.Copy
import com.samliothek.catalog.domain.CopyStatus
import com.samliothek.shared.Barcode
import com.samliothek.shared.Isbn
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId
import com.samliothek.support.requireIsbn
import com.samliothek.support.requireSuccess
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Use-case tests with in-memory fakes (no Spring, no database).
 */
class CatalogUseCaseTest {
    private lateinit var books: InMemoryBookCatalog
    private lateinit var copies: InMemoryCopyCatalog
    private lateinit var registerBook: RegisterBookUseCase
    private lateinit var getBook: GetBookUseCase
    private lateinit var searchBooks: SearchBooksUseCase
    private lateinit var addCopy: AddCopyUseCase
    private lateinit var listCopies: ListCopiesUseCase

    @BeforeEach
    fun setUp() {
        books = InMemoryBookCatalog()
        copies = InMemoryCopyCatalog()
        registerBook = RegisterBookService(books)
        getBook = GetBookService(books)
        searchBooks = SearchBooksService(books)
        addCopy = AddCopyService(books, copies)
        listCopies = ListCopiesService(books, copies)
    }

    @Test
    fun `register book stores a new title-level record`() {
        val result = registerBook.register(cleanCodeCommand())

        result.shouldBeInstanceOf<Outcome.Success<Book>>()
        val book = result.value
        book.title shouldBe "Clean Code"
        books.findById(book.id) shouldBe book
    }

    @Test
    fun `register book rejects a duplicate isbn`() {
        registerBook.register(cleanCodeCommand())

        val result = registerBook.register(cleanCodeCommand())

        result.shouldBeInstanceOf<Outcome.Failure<CatalogError>>()
        result.error.shouldBeInstanceOf<CatalogError.DuplicateIsbn>()
    }

    @Test
    fun `get book returns not found for unknown id`() {
        val missing = BookId(UUID.fromString("99999999-9999-9999-9999-999999999999"))

        val result = getBook.get(missing)

        result.shouldBeInstanceOf<Outcome.Failure<CatalogError>>()
        result.error shouldBe CatalogError.BookNotFound(missing)
    }

    @Test
    fun `search books matches title or author case-insensitively`() {
        registerBook.register(cleanCodeCommand())
        registerBook.register(
            RegisterBookCommand(
                isbn = isbn("9780201633610"),
                title = "Design Patterns",
                author = "Erich Gamma",
                publishedYear = 1994,
            ),
        )

        val byTitle = searchBooks.search(searchQuery("clean"))
        val byAuthor = searchBooks.search(searchQuery("gamma"))

        byTitle.items.shouldHaveSize(1)
        byTitle.items.single().title shouldBe "Clean Code"
        byAuthor.items.shouldHaveSize(1)
        byAuthor.items.single().author shouldBe "Erich Gamma"
    }

    @Test
    fun `search books paginates results`() {
        registerBook.register(
            RegisterBookCommand(
                isbn = isbn("9780132350884"),
                title = "Alpha Book",
                author = "Author A",
                publishedYear = 2001,
            ),
        )
        registerBook.register(
            RegisterBookCommand(
                isbn = isbn("9780201633610"),
                title = "Beta Book",
                author = "Author B",
                publishedYear = 2002,
            ),
        )
        registerBook.register(
            RegisterBookCommand(
                isbn = isbn("9780596007126"),
                title = "Gamma Book",
                author = "Author C",
                publishedYear = 2003,
            ),
        )

        val firstPage = searchBooks.search(searchQuery(page = 0, size = 2))
        val secondPage = searchBooks.search(searchQuery(page = 1, size = 2))

        firstPage.page shouldBe 0
        firstPage.size shouldBe 2
        firstPage.items.shouldHaveSize(2)
        firstPage.items.map { it.title } shouldBe listOf("Alpha Book", "Beta Book")

        secondPage.page shouldBe 1
        secondPage.size shouldBe 2
        secondPage.items.shouldHaveSize(1)
        secondPage.items.single().title shouldBe "Gamma Book"
    }

    @Test
    fun `add copy fails when book does not exist`() {
        val missing = BookId(UUID.fromString("99999999-9999-9999-9999-999999999999"))

        val result = addCopy.add(AddCopyCommand(bookId = missing, barcode = Barcode("BIB-000123")))

        result.shouldBeInstanceOf<Outcome.Failure<CatalogError>>()
        result.error shouldBe CatalogError.BookNotFound(missing)
    }

    @Test
    fun `add copy creates an available physical copy`() {
        val book = registerBook.register(cleanCodeCommand()).requireSuccess()

        val result = addCopy.add(AddCopyCommand(bookId = book.id, barcode = Barcode("BIB-000123")))

        result.shouldBeInstanceOf<Outcome.Success<Copy>>()
        result.value.status shouldBe CopyStatus.AVAILABLE
        result.value.bookId shouldBe book.id
    }

    @Test
    fun `list copies returns copies for a book`() {
        val book = registerBook.register(cleanCodeCommand()).requireSuccess()
        addCopy.add(AddCopyCommand(bookId = book.id, barcode = Barcode("BIB-000123")))
        addCopy.add(AddCopyCommand(bookId = book.id, barcode = Barcode("BIB-000124")))

        val result = listCopies.list(book.id)

        result.shouldBeInstanceOf<Outcome.Success<List<Copy>>>()
        result.value.shouldHaveSize(2)
    }

    @Test
    fun `list copies fails when book does not exist`() {
        val missing = BookId(UUID.fromString("99999999-9999-9999-9999-999999999999"))

        val result = listCopies.list(missing)

        result.shouldBeInstanceOf<Outcome.Failure<CatalogError>>()
        result.error shouldBe CatalogError.BookNotFound(missing)
    }

    private fun cleanCodeCommand() =
        RegisterBookCommand(
            isbn = isbn("9780132350884"),
            title = "Clean Code",
            author = "Robert C. Martin",
            publishedYear = 2008,
        )

    private fun isbn(raw: String): Isbn = requireIsbn(raw)

    private fun searchQuery(
        query: String = "",
        page: Int = 0,
        size: Int = 20,
    ): SearchBooksQuery = SearchBooksQuery.of(query = query, page = page, size = size).requireSuccess()
}

private class InMemoryBookCatalog : BookCatalog {
    private val byId = ConcurrentHashMap<BookId, Book>()
    private val byIsbn = ConcurrentHashMap<String, BookId>()

    override fun findById(id: BookId): Book? = byId[id]

    override fun findByIsbn(isbn: Isbn): Book? = byIsbn[isbn.value]?.let { byId[it] }

    override fun search(
        query: String,
        page: Int,
        size: Int,
    ): List<Book> {
        val needle = query.trim().lowercase()
        val filtered =
            byId.values
                .filter {
                    needle.isEmpty() ||
                        it.title.lowercase().contains(needle) ||
                        it.author.lowercase().contains(needle)
                }.sortedBy { it.title }
        val from = page * size
        if (from >= filtered.size) return emptyList()
        return filtered.subList(from, minOf(from + size, filtered.size))
    }

    override fun save(book: Book) {
        byId[book.id] = book
        byIsbn[book.isbn.value] = book.id
    }
}

private class InMemoryCopyCatalog : CopyCatalog {
    private val byId = ConcurrentHashMap<CopyId, Copy>()

    override fun save(copy: Copy) {
        byId[copy.id] = copy
    }

    override fun findByBookId(bookId: BookId): List<Copy> = byId.values.filter { it.bookId == bookId }

    override fun findByBarcode(barcode: Barcode): Copy? = byId.values.firstOrNull { it.barcode == barcode }
}
