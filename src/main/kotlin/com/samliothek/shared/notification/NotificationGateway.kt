package com.samliothek.shared.notification

import com.samliothek.shared.Outcome

data class NotificationMessage(
    val recipientEmail: String,
    val subject: String,
    val body: String,
    val correlationId: String,
)

fun interface NotificationGateway {
    /** Enqueue for delivery. Failure means the external queue is unreachable. */
    fun enqueue(message: NotificationMessage): Outcome<NotificationError, Unit>
}

sealed interface NotificationError {
    data class QueueUnavailable(
        val detail: String,
    ) : NotificationError
}
