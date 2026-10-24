package com.samliothek.catalog.adapter.`in`.web

import com.samliothek.catalog.application.port.`in`.AddCopyUseCase
import com.samliothek.catalog.application.port.`in`.BookPage
import com.samliothek.catalog.application.port.`in`.GetBookUseCase
import com.samliothek.catalog.application.port.`in`.ListCopiesUseCase
import com.samliothek.catalog.application.port.`in`.RegisterBookUseCase
import com.samliothek.catalog.application.port.`in`.SearchBooksUseCase
import com.samliothek.catalog.domain.Book
import com.samliothek.catalog.domain.CatalogError
import com.samliothek.catalog.domain.Copy
import com.samliothek.catalog.domain.CopyStatus
import com.samliothek.shared.Barcode
import com.samliothek.shared.Outcome
import com.samliothek.shared.ids.BookId
import com.samliothek.shared.ids.CopyId
import com.samliothek.support.requireIsbn
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

/**
 * HTTP adapter tests. Use cases are mocked — no database.
 * `@RestControllerAdvice` beans are picked up by the WebMvc slice automatically.
 */
@WebMvcTest(controllers = [BookController::class])
class BookControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var registerBook: RegisterBookUseCase

    @MockitoBean
    private lateinit var getBook: GetBookUseCase

    @MockitoBean
    private lateinit var searchBooks: SearchBooksUseCase

    @MockitoBean
    private lateinit var addCopy: AddCopyUseCase

    @MockitoBean
    private lateinit var listCopies: ListCopiesUseCase

    private val bookId = BookId(UUID.fromString("6f1c1111-1111-1111-1111-111111111111"))
    private val copyId = CopyId(UUID.fromString("a83d2222-2222-2222-2222-222222222222"))
    private val isbn = requireIsbn("9780132350884")

    @Test
    fun `POST books returns 201 with the created book`() {
        val book = Book(bookId, isbn, "Testing Book", "Sample writer", 2008)
        given(registerBook.register(any())).willReturn(Outcome.Success(book))

        mockMvc
            .perform(
                post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"isbn":"9780132350884","title":"Testing Book",
                         "author":"Sample writer","publishedYear":2008}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(bookId.value.toString()))
            .andExpect(jsonPath("$.isbn").value("9780132350884"))
            .andExpect(jsonPath("$.title").value("Testing Book"))
    }

    @Test
    fun `POST books returns 400 for invalid isbn`() {
        mockMvc
            .perform(
                post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"isbn":"bad","title":"Testing Book",
                         "author":"Sample writer","publishedYear":2008}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/validation"))
    }

    @Test
    fun `POST books returns 409 for duplicate isbn`() {
        given(registerBook.register(any()))
            .willReturn(Outcome.Failure(CatalogError.DuplicateIsbn(isbn)))

        mockMvc
            .perform(
                post("/api/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"isbn":"9780132350884","title":"Testing Book",
                         "author":"Sample writer","publishedYear":2008}
                        """.trimIndent(),
                    ),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/duplicate-isbn"))
    }

    @Test
    fun `GET book returns 404 when missing`() {
        given(getBook.get(bookId)).willReturn(Outcome.Failure(CatalogError.BookNotFound(bookId)))

        mockMvc
            .perform(get("/api/books/${bookId.value}"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/book-not-found"))
    }

    @Test
    fun `GET books returns search results`() {
        val book = Book(bookId, isbn, "Testing Book", "Sample writer", 2008)
        given(searchBooks.search(any())).willReturn(BookPage(items = listOf(book), page = 0, size = 20))

        mockMvc
            .perform(get("/api/books").param("query", "testing"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].title").value("Testing Book"))
    }

    @Test
    fun `GET books returns 400 for invalid page size`() {
        mockMvc
            .perform(get("/api/books").param("size", "0"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("https://samliothek.dev/errors/validation"))
    }

    @Test
    fun `POST copies returns 201`() {
        val copy = Copy(copyId, bookId, Barcode("BIB-000123"), CopyStatus.AVAILABLE)
        given(addCopy.add(any())).willReturn(Outcome.Success(copy))

        mockMvc
            .perform(
                post("/api/books/${bookId.value}/copies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"barcode":"BIB-000123"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.barcode").value("BIB-000123"))
            .andExpect(jsonPath("$.status").value("AVAILABLE"))
    }

    @Test
    fun `GET copies returns list`() {
        val copy = Copy(copyId, bookId, Barcode("BIB-000123"), CopyStatus.AVAILABLE)
        given(listCopies.list(bookId)).willReturn(Outcome.Success(listOf(copy)))

        mockMvc
            .perform(get("/api/books/${bookId.value}/copies"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].barcode").value("BIB-000123"))
    }
}
