package com.samliothek.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.rabbitmq.RabbitMQContainer

/**
 * Shared RabbitMQ Testcontainers wiring for notification adapter tests.
 */
@Testcontainers
abstract class RabbitIntegrationTest : PostgresIntegrationTest() {
    companion object {
        @Container
        @JvmStatic
        val rabbit: RabbitMQContainer =
            RabbitMQContainer("rabbitmq:4-management")

        @DynamicPropertySource
        @JvmStatic
        fun registerRabbit(registry: DynamicPropertyRegistry) {
            registry.add("spring.rabbitmq.host", rabbit::getHost)
            registry.add("spring.rabbitmq.port") { rabbit.getAmqpPort().toString() }
            registry.add("spring.rabbitmq.username", rabbit::getAdminUsername)
            registry.add("spring.rabbitmq.password", rabbit::getAdminPassword)
        }
    }
}
