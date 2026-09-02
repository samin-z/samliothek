package com.samliothek.notification

import com.samliothek.shared.Outcome
import com.samliothek.shared.notification.NotificationGateway
import com.samliothek.shared.notification.NotificationMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Phase 0 / local stub. Phase 4 replaces the active profile bean with the RabbitMQ adapter.
 * Checkout must never depend on this succeeding.
 */
@Component
class LoggingNotificationGateway : NotificationGateway {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun enqueue(message: NotificationMessage): Outcome<com.samliothek.shared.notification.NotificationError, Unit> {
        log.info(
            "notification queued (log stub): to={} subject={} correlationId={}",
            message.recipientEmail,
            message.subject,
            message.correlationId,
        )
        return Outcome.Success(Unit)
    }
}
