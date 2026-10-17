package com.samliothek.catalog.adapter.`in`.web

import com.samliothek.catalog.adapter.`in`.web.dto.AddCopyRequest
import com.samliothek.catalog.adapter.`in`.web.dto.RegisterBookRequest
import com.samliothek.catalog.application.port.`in`.AddCopyCommand
import com.samliothek.catalog.application.port.`in`.AddCopyUseCase
import com.samliothek.catalog.application.port.`in`.GetBookUseCase
import com.samliothek.catalog.application.port.`in`.ListCopiesUseCase
import com.samliothek.catalog.application.port.`in`.RegisterBookCommand
import com.samliothek.catalog.application.port.`in`.RegisterBookUseCase
import com.samliothek.catalog.application.port.`in`.SearchBooksQuery
import com.samliothek.catalog.application.port.`in`.SearchBooksUseCase
import com.samliothek.shared.Barcode
import com.samliothek.shared.Isbn
import com.samliothek.shared.Outcome
import com.samliothek.shared.fold
import com.samliothek.shared.ids.BookId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/books")
class BookController(
    private val registerBook: RegisterBookUseCase,
    private val getBook: GetBookUseCase,
    private val searchBooks: SearchBooksUseCase,
    private val addCopy: AddCopyUseCase,
    private val listCopies: ListCopiesUseCase,
    private val advice: CatalogExceptionAdvice,
) {
    @PostMapping
    fun register(
        @Valid @RequestBody request: RegisterBookRequest,
    ): ResponseEntity<*> {
        val isbn =
            when (val parsed = Isbn.of(request.isbn)) {
                is Outcome.Failure -> return advice.validationProblem(parsed.error)
                is Outcome.Success -> parsed.value
            }
        return registerBook
            .register(
                RegisterBookCommand(
                    isbn = isbn,
                    title = request.title,
                    author = request.author,
                    publishedYear = request.publishedYear,
                ),
            ).fold(
                onFailure = { advice.toProblem(it) },
                onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it.toResponse()) },
            )
    }

    @GetMapping("/{bookId}")
    fun get(
        @PathVariable bookId: UUID,
    ): ResponseEntity<*> =
        getBook.get(BookId(bookId)).fold(
            onFailure = { advice.toProblem(it) },
            onSuccess = { ResponseEntity.ok(it.toResponse()) },
        )

    @GetMapping
    fun search(
        @RequestParam(required = false, defaultValue = "") query: String,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<*> {
        val searchQuery =
            when (val parsed = SearchBooksQuery.of(query = query, page = page, size = size)) {
                is Outcome.Failure -> return advice.validationProblem(parsed.error)
                is Outcome.Success -> parsed.value
            }
        val result = searchBooks.search(searchQuery)
        return ResponseEntity.ok(result.items.toPageResponse(result.page, result.size))
    }

    @PostMapping("/{bookId}/copies")
    fun addCopy(
        @PathVariable bookId: UUID,
        @Valid @RequestBody request: AddCopyRequest,
    ): ResponseEntity<*> =
        addCopy
            .add(AddCopyCommand(bookId = BookId(bookId), barcode = Barcode(request.barcode.trim())))
            .fold(
                onFailure = { advice.toProblem(it) },
                onSuccess = { ResponseEntity.status(HttpStatus.CREATED).body(it.toResponse()) },
            )

    @GetMapping("/{bookId}/copies")
    fun listCopies(
        @PathVariable bookId: UUID,
    ): ResponseEntity<*> =
        listCopies.list(BookId(bookId)).fold(
            onFailure = { advice.toProblem(it) },
            onSuccess = { copies -> ResponseEntity.ok(copies.map { it.toResponse() }) },
        )
}
