/**
 * Notification module: listens to lending events and enqueues via shared NotificationGateway.
 * No domain layer by design.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "shared", "lending" }
)
package com.samliothek.notification;
