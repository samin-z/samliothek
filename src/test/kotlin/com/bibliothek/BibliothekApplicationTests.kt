package com.bibliothek

import com.bibliothek.support.PostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class BibliothekApplicationTests : PostgresIntegrationTest() {
    @Test
    fun `application context loads against real PostgreSQL`() {
        // Intentionally empty — context load is the assertion.
    }
}
