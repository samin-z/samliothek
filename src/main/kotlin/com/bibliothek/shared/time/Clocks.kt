package com.bibliothek.shared.time

import java.time.Clock

/**
 * Marker so domain/application code injects Clock instead of Instant.now().
 * Bound to the system UTC clock in configuration.
 */
object Clocks {
    fun utc(): Clock = Clock.systemUTC()
}
