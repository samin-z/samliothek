package com.samliothek.catalog.adapter.out.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("books")
data class BookRecord(
    @Id val id: UUID,
    val isbn: String,
    val title: String,
    val author: String,
    @Column("published_year") val publishedYear: Int,
    @Column("created_at") val createdAt: Instant,
    /** Null marks a new aggregate so client-assigned UUIDs still INSERT. */
    @Version val version: Long? = null,
)

@Table("copies")
data class CopyRecord(
    @Id val id: UUID,
    @Column("book_id") val bookId: UUID,
    val barcode: String,
    val status: String,
    @Column("created_at") val createdAt: Instant,
    @Version val version: Long? = null,
)
