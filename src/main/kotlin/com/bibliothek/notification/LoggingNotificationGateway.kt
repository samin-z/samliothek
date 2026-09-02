package com.bibliothek.notification

import com.bibliothek.shared.Outcome
import com.bibliothek.shared.notification.NotificationGateway
import com.bibliothek.shared.notification.NotificationMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Phase 0 / local stub. Phase 4 replaces the active profile bean with the RabbitMQ adapter.
 * Checkout must never depend on this succeeding.
 */
@Component
class LoggingNotificationGateway : NotificationGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun enqueue(message: NotificationMessage): Outcome<com.bibliothek.shared.notification.NotificationError, Unit> {
        log.info(
            "notification queued (log stub): to={} subject={} correlationId={}",
            message.recipientEmail,
            message.subject,
            message.correlationId,
        )
        return Outcome.Success(Unit)
    }
}
