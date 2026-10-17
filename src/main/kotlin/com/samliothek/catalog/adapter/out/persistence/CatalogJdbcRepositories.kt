package com.samliothek.catalog.adapter.out.persistence

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

internal interface BookJdbcRepository : CrudRepository<BookRecord, UUID> {
    fun findByIsbn(isbn: String): BookRecord?

    @Query(
        """
        SELECT * FROM books
        WHERE title ILIKE :pattern OR author ILIKE :pattern
        ORDER BY title
        LIMIT :limit OFFSET :offset
        """,
    )
    fun searchByPattern(
        @Param("pattern") pattern: String,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int,
    ): List<BookRecord>

    @Query(
        """
        SELECT * FROM books
        ORDER BY title
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findPage(
        @Param("limit") limit: Int,
        @Param("offset") offset: Int,
    ): List<BookRecord>
}

internal interface CopyJdbcRepository : CrudRepository<CopyRecord, UUID> {
    fun findByBookId(bookId: UUID): List<CopyRecord>

    fun findByBarcode(barcode: String): CopyRecord?
}
