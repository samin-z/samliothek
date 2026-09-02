# Samliothek

Library lending service — modular monolith. See [PROJECT.md](PROJECT.md) for the contract.

## Stack

Kotlin · Spring Boot 4.1 · Spring Modulith 2.1 · Spring Data JDBC · PostgreSQL 17 · RabbitMQ 4 · Flyway · Testcontainers

## Prerequisites

- JDK **25** (runtime / toolchain) and a JDK **21–24** for the Gradle daemon (detekt 1.23 cannot parse JDK 25)
- Docker (Compose v2) — required for local Postgres/RabbitMQ and for Testcontainers

```bash
# Prefer Temurin 24 for Gradle; toolchain still compiles with 25
export JAVA_HOME=$(/usr/libexec/java_home -v 24)
java -version          # 24.x for Gradle
docker compose version # v2.x
```

## First run

```bash
docker compose up -d
./gradlew build
./gradlew bootRun
open http://localhost:8080/swagger-ui.html
```

OpenAPI is public — no authentication or authorisation (deliberate).
