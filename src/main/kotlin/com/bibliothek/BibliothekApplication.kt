package com.bibliothek

import com.bibliothek.shared.time.Clocks
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import java.time.Clock

@SpringBootApplication
@EnableAsync
class BibliothekApplication {
    @Bean
    fun clock(): Clock = Clocks.utc()
}

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<BibliothekApplication>(*args)
}
