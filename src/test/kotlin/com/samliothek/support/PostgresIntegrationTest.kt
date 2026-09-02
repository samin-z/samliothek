package com.samliothek.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Shared Postgres Testcontainers wiring. Prefer this over copy-pasting containers per class.
 */
@Testcontainers
abstract class PostgresIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer("postgres:17")
                .withDatabaseName("samliothek")
                .withUsername("samliothek")
                .withPassword("samliothek")

        @DynamicPropertySource
        @JvmStatic
        fun registerDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.docker.compose.enabled") { "false" }
        }
    }
}
