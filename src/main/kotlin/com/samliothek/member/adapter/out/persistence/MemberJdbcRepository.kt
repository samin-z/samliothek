package com.samliothek.member.adapter.out.persistence

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import java.util.UUID

internal interface MemberJdbcRepository : CrudRepository<MemberRecord, UUID> {
    fun findByEmail(email: String): MemberRecord?

    @Query(
        """
        SELECT * FROM members
        ORDER BY name
        LIMIT :limit OFFSET :offset
        """,
    )
    fun findPage(
        @Param("limit") limit: Int,
        @Param("offset") offset: Int,
    ): List<MemberRecord>
}
