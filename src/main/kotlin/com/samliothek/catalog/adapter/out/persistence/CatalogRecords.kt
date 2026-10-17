package com.samliothek.catalog.adapter.out.persistence

import com.samliothek.catalog.domain.CopyStatus
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.UUID

@Table("books")
internal data class BookRecord(
    @Id val id: UUID,
    val isbn: String,
    val title: String,
    val author: String,
    @Column("published_year") val publishedYear: Int,
    @ReadOnlyProperty
    @Column("created_at")
    val createdAt: Instant? = null,
    /** Null marks a new aggregate so client-assigned UUIDs still INSERT. */
    @Version val version: Long? = null,
)

@Table("copies")
internal data class CopyRecord(
    @Id val id: UUID,
    @Column("book_id") val bookId: UUID,
    val barcode: String,
    val status: CopyStatus,
    @ReadOnlyProperty
    @Column("created_at")
    val createdAt: Instant? = null,
    @Version val version: Long? = null,
)
