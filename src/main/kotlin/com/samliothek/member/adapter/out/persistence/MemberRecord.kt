package com.samliothek.member.adapter.out.persistence

import com.samliothek.member.domain.MembershipTier
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table("members")
internal data class MemberRecord(
    @Id val id: UUID,
    val name: String,
    val email: String,
    val tier: MembershipTier,
    @Column("joined_on") val joinedOn: LocalDate,
    @ReadOnlyProperty
    @Column("created_at")
    val createdAt: Instant? = null,
    /** Null marks a new aggregate so client-assigned UUIDs still INSERT. */
    @Version val version: Long? = null,
)
