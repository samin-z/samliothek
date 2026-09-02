package com.samliothek

import com.samliothek.shared.time.Clocks
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import java.time.Clock

@SpringBootApplication
@EnableAsync
class SamliothekApplication {
    @Bean
    fun clock(): Clock = Clocks.utc()
}

@Suppress("SpreadOperator")
fun main(args: Array<String>) {
    runApplication<SamliothekApplication>(*args)
}
