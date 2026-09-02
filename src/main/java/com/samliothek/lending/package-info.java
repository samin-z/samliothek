/**
 * Lending module: loans, borrowing rules, and borrowing history.
 * Cross-module ports to catalog/member are added in Phase 3 via NamedInterfaces.
 */
@org.springframework.modulith.ApplicationModule(
    allowedDependencies = { "shared" }
)
package com.samliothek.lending;
