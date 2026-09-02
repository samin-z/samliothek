package com.bibliothek.shared.problem

import org.springframework.http.ProblemDetail
import java.net.URI

/**
 * Zalando-style RFC 9457 helpers on Spring's first-party ProblemDetail.
 * Zalando problem-spring-web is maintenance-mode; we keep the conventions, not the dependency.
 */
object Problems {
    private const val TYPE_BASE = "https://bibliothek.dev/errors"

    fun of(
        status: Int,
        typeSlug: String,
        title: String,
        detail: String,
        instance: String,
    ): ProblemDetail =
        ProblemDetail.forStatus(status).apply {
            type = URI.create("$TYPE_BASE/$typeSlug")
            this.title = title
            this.detail = detail
            this.instance = URI.create(instance)
        }

    fun queueUnavailable(
        detail: String,
        instance: String,
    ): ProblemDetail =
        of(
            status = 503,
            typeSlug = "queue-unavailable",
            title = "Notification queue unavailable",
            detail = detail,
            instance = instance,
        )
}
