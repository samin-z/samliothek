package com.samliothek

import com.samliothek.support.PostgresIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class SamliothekApplicationTests : PostgresIntegrationTest() {
    @Test
    fun `application context loads against real PostgreSQL`() {
        // Intentionally empty — context load is the assertion.
    }
}
