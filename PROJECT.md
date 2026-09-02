# Bibliothek — Project Definition

**Stack:** Kotlin 2.3+ (Boot-managed; 2.4 when Boot 4.2 lands) · Spring Boot 4.1 · Spring Modulith 2.1 · Spring Data JDBC · PostgreSQL 17 · RabbitMQ 4 · Flyway · Gradle 9 (Kotlin DSL)
**Architecture:** Modular monolith — 5 application modules, each internally hexagonal
**Scope:** Small on purpose. 4 aggregates, 12 endpoints, ~8–11 focused days.

> This document is the contract. If the code and this file disagree, one of them is a bug.
> Update this file in the same commit as the change.

---

## 1. What we are building

A library lending service. Members borrow physical copies of books and return them.
That's it.

The domain is deliberately small but **not CRUD** — there are three rules that interact,
and those three rules are the entire justification for hexagonal architecture and the
SOLID work. Without them this would be a REST wrapper over four tables and none of the
structure would be earning its keep.

### The three rules

| # | Rule | Where it lives |
| --- | --- | --- |
| R1 | A member may not exceed their tier's loan limit (Standard 3, Premium 10) | `LoanLimitRule` — a `BorrowingRule` in `lending` |
| R2 | A member with any overdue loan may not borrow | `NoOverdueLoansRule` — a `BorrowingRule` in `lending` |
| R3 | Loan period depends on tier (Standard 14 days, Premium 28 days) | `TieredLoanPeriodPolicy` — a `LoanPeriodPolicy` in `lending` |

R1 and R2 are pluggable strategies composed in a list. Adding a fourth rule means adding
one file and touching nothing else. **That is the Open/Closed demonstration, and it is the
reason these particular rules were chosen.**

### Out of scope — deliberately

Written down so it reads as a decision, not an oversight. Do not add these:

- ❌ Holds / reservation queues
- ❌ Overdue fines and payments
- ❌ Renewals
- ❌ Authentication, authorisation, users-vs-members
- ❌ Multi-branch libraries
- ❌ Search beyond a simple title/author `LIKE`
- ❌ Frontend of any kind

If the project finishes early, the right move is **more tests**, not more features.

---

## 2. Modules

Spring Modulith defines an application module as a **direct subpackage of the main
application package**. Anything in a nested package below it is internal to that module
and invisible to the others. That default does most of the encapsulation work for free.

| Module | Owns | Has web API? | Has domain layer? |
| --- | --- | --- | --- |
| `shared` | Identifiers, `Isbn`, `Email`, `Outcome`, clock, **notification contracts**, RFC 9457 helpers | ❌ | — (open module) |
| `catalog` | Books, physical copies | ✅ | ✅ |
| `member` | Members and their tier | ✅ | ✅ (thin) |
| `lending` | Loans + **borrowing history** read model. **The core.** | ✅ | ✅ (rich) |
| `notification` | Reacts to lending events → enqueues via shared gateway | ❌ | ❌ **by design** |

> **`notification` gets no domain layer and no ports-and-adapters ceremony.** It has no
> business rules — it listens and it sends. Applying the full hexagon to it would be the
> single most common way a project in this style goes wrong. Architecture is a budget;
> spend it where the complexity actually is.
>
> **Notification contracts live in `shared`.** The `NotificationGateway` port, message
> types, and queue-failure problem helpers sit in `shared` so any module can publish
> without depending on the `notification` adapter. The `notification` *module* stays thin:
> it listens to `lending` events and calls the shared gateway. The listener cannot move
> into `shared` — that would reverse the dependency graph (`shared` must never depend on
> `lending`).

### Allowed dependencies

```
notification ──► lending ──► shared
                    │
   catalog ─────────┼──────► shared
                    │
   member  ─────────┘
```

- `catalog` and `member` know **nothing** about `lending`.
- `lending` does **not** import `catalog` or `member` domain types. It declares its own
  output ports (`CopyRegistry`, `MemberDirectory`) in its own vocabulary, and an adapter
  inside `lending` implements them against the other modules' named interfaces. See §7.
- `notification` depends on `lending`'s published events and `shared`'s `NotificationGateway`.

Enforced by `ApplicationModules.of(...).verify()` — a failing test, not a convention.

---

## 3. Prerequisites — what to install

| Tool | Version | Install |
| --- | --- | --- |
| JDK | **25 (LTS)** | `sdk install java 25-tem` (SDKMAN) or Temurin from adoptium.net |
| Docker | 24+ with Compose v2 | Docker Desktop, or `docker` + `docker-compose-plugin` |
| Gradle | — | **Do not install.** Use the wrapper (`./gradlew`) |
| IntelliJ IDEA | 2026.1+ | Community edition is sufficient |

Verify before starting:

```bash
java -version          # openjdk 25
docker info            # must not error — Testcontainers needs a running daemon
docker compose version # v2.x
```

Optional but useful: `psql` (client only) for poking at the database, `httpie` or `curl`
for hitting the API.

### First run

```bash
git clone <repo> && cd bibliothek
docker compose up -d              # postgres on :5432, rabbitmq on :5672 / :15672
./gradlew build                   # compiles, runs unit + arch + integration tests
./gradlew bootRun                 # http://localhost:8080
open http://localhost:8080/swagger-ui.html
```

`./gradlew build` starting Testcontainers means Docker must be up. That is intentional —
we test against real PostgreSQL (and RabbitMQ where relevant), never H2.

---

## 4. Technology choices

| Concern | Choice | Why |
| --- | --- | --- |
| Language | Kotlin 2.3+ (Boot-managed) | Null-safety in the type system, `value class` for IDs, `sealed` for exhaustive state, classes `final` by default, `internal` visibility. Each one is a SOLID guard the compiler gives us for free. Kotlin 2.4 lands with Boot 4.2 — do not pin ahead of the BOM. |
| JDK | Java 25 LTS | Virtual threads make blocking MVC fine again. Boot 4's baseline is 17; 25 is supported. |
| Framework | Spring Boot 4.1 | Confined to the adapter ring. The domain never imports it. |
| Modularity | Spring Modulith 2.1 | Boundary verification as a test, transactional event outbox, generated architecture docs |
| Persistence | **Spring Data JDBC — not JPA** | See below. This is the most consequential choice here. |
| Database | PostgreSQL 17 | Real constraints, real dialect |
| Migrations | Flyway, plain SQL | Explicit schema ownership. Boot 4's module split means the Flyway starter must be declared explicitly. |
| Messaging (in-process) | Spring Modulith events + JDBC outbox | Domain events (`CopyCheckedOut`, `CopyReturned`) stay inside the monolith; publication is durable until listeners complete |
| Messaging (external) | **RabbitMQ 4 + Spring AMQP** | Notifications leave the process via an external queue. Right-sized for this domain — Kafka would be ceremony without a driver |
| Web | Spring MVC on virtual threads | Simpler than WebFlux; virtual threads remove the "but blocking I/O" objection. Reactive here would be complexity with no driver. |
| API errors | RFC 9457 via Spring `ProblemDetail` | Zalando `problem-spring-web` is maintenance-mode (Spring owns RFC 9457 natively). We follow Zalando's *conventions* — typed error URIs, `application/problem+json`, one advice per module — on Spring's built-in type. Helpers live in `shared`. |
| API docs | springdoc-openapi 3.x | Swagger UI at `/swagger-ui.html`. **No authentication / authorisation** — OpenAPI is fully public; Spring Security is not on the classpath (see §1 out of scope) |
| Tests | JUnit 5 · Kotest assertions · **hand-written fakes** · **Testcontainers** (PostgreSQL + RabbitMQ) · ArchUnit · Konsist | Mocking frameworks hide the pain that fat ports are supposed to cause. If a port needs MockK, the port is too big. |
| Quality | ktlint · detekt · Kover | detekt's complexity thresholds are a decent SRP proxy |

### Why Spring Data JDBC and not JPA

JPA actively fights a pure domain model:

- Lazy loading needs proxies → needs non-final classes → needs the `all-open` and `no-arg`
  Kotlin compiler plugins. You disable Kotlin's LSP protection to satisfy your ORM.
- `data class` + `@Entity` is a known trap: generated `equals`/`hashCode` over a nullable
  `id` breaks proxy identity and `Set` membership.
- Dirty checking means "when does this get saved?" stops being answerable by reading the code.

Spring Data JDBC is aggregate-oriented: load, mutate, explicitly `save`. No proxies, no
session, no surprises. The domain stays plain Kotlin; persistence records live in
`adapter/out/persistence` with a hand-written mapper between them.

**The cost, stated honestly:** you write the mapping by hand, and complex read queries need
their own path. Both are fine. The mapper is boring code a ten-line test pins down, and
read queries shouldn't route through aggregates anyway — they get a dedicated query port
returning DTOs directly.

---

## 5. Domain models

All domain types are plain Kotlin. **No annotations from Spring, Jakarta, or Jackson appear
anywhere in this section.** That is checked by an ArchUnit test.

### 5.1 `shared`

```kotlin
@JvmInline value class BookId(val value: UUID)
@JvmInline value class CopyId(val value: UUID)
@JvmInline value class MemberId(val value: UUID)
@JvmInline value class LoanId(val value: UUID)

@JvmInline
value class Isbn private constructor(val value: String) {
    companion object {
        private val PATTERN = Regex("^\\d{13}$")
        fun of(raw: String): Outcome<ValidationError, Isbn> {
            val normalised = raw.replace("-", "").trim()
            return if (PATTERN.matches(normalised)) Success(Isbn(normalised))
                   else Failure(ValidationError("isbn must be 13 digits"))
        }
    }
}

@JvmInline value class Barcode(val value: String)
@JvmInline value class Email private constructor(val value: String) { /* same shape */ }
```

**The `Outcome` type.** Kotlin's stdlib `Result<T>` only carries a `Throwable`, so it can't
express typed domain errors. We hand-roll a ~20-line sealed type:

```kotlin
sealed interface Outcome<out E, out A> {
    data class Success<A>(val value: A) : Outcome<Nothing, A>
    data class Failure<E>(val error: E) : Outcome<E, Nothing>
}

inline fun <E, A, B> Outcome<E, A>.map(f: (A) -> B): Outcome<E, B>
inline fun <E, A, B> Outcome<E, A>.flatMap(f: (A) -> Outcome<E, B>): Outcome<E, B>
inline fun <E, A, B> Outcome<E, A>.fold(onFailure: (E) -> B, onSuccess: (A) -> B): B
```

*Arrow's `Either` is the grown-up option and the right call on a real project. For a
project this size, writing the 20 lines is cheaper than the dependency and more
instructive.* **Whichever you pick, never mix it with exceptions for domain errors** —
that's worse than either alone.

Also in `shared`: `Clock` is injected everywhere, never `Instant.now()` in domain code.
Untestable time is the most common reason a "pure" domain isn't.

**Notification contracts** (port + messages — no Spring AMQP types here):

```kotlin
data class NotificationMessage(
    val recipientEmail: String,
    val subject: String,
    val body: String,
    val correlationId: String,
)

fun interface NotificationGateway {
    /** Enqueue for delivery. Failure means the external queue is unreachable. */
    fun enqueue(message: NotificationMessage): Outcome<NotificationError, Unit>
}

sealed interface NotificationError {
    data class QueueUnavailable(val detail: String) : NotificationError
}
```

**RFC 9457 helpers** in `shared` (Zalando-style conventions on Spring's `ProblemDetail`):

```kotlin
object Problems {
    fun of(status: Int, type: String, title: String, detail: String, instance: String): ProblemDetail
}
// type URIs look like: https://bibliothek.dev/errors/queue-unavailable
```

> Zalando's `problem-spring-web` is explicitly in maintenance mode because Spring Framework
> now owns RFC 9457. We do **not** add that dependency. We do adopt its shape: typed
> `type` URIs, stable titles, and `application/problem+json` everywhere — including when
> the notification queue is down (`503` + `queue-unavailable`).

### 5.2 `catalog`

```kotlin
data class Book(
    val id: BookId,
    val isbn: Isbn,
    val title: String,
    val author: String,
    val publishedYear: Int,
)

data class Copy(
    val id: CopyId,
    val bookId: BookId,
    val barcode: Barcode,
    val status: CopyStatus,
) {
    fun markOnLoan(): Outcome<CatalogError, Copy> =
        if (status == CopyStatus.AVAILABLE) Success(copy(status = CopyStatus.ON_LOAN))
        else Failure(CatalogError.CopyNotAvailable(id))

    fun markAvailable(): Copy = copy(status = CopyStatus.AVAILABLE)
}

enum class CopyStatus { AVAILABLE, ON_LOAN, WITHDRAWN }

sealed interface CatalogError {
    data class BookNotFound(val id: BookId) : CatalogError
    data class DuplicateIsbn(val isbn: Isbn) : CatalogError
    data class CopyNotFound(val barcode: Barcode) : CatalogError
    data class CopyNotAvailable(val id: CopyId) : CatalogError
}
```

> Note `markOnLoan()` lives **on the `Copy` type**, not in a service. This is exactly
> finding #5 from your last code review ("status transition logic in the service") fixed
> at design time rather than in review.

### 5.3 `member`

```kotlin
data class Member(
    val id: MemberId,
    val name: String,
    val email: Email,
    val tier: MembershipTier,
    val joinedOn: LocalDate,
)

enum class MembershipTier { STANDARD, PREMIUM }

sealed interface MemberError {
    data class NotFound(val id: MemberId) : MemberError
    data class DuplicateEmail(val email: Email) : MemberError
}
```

> **`MembershipTier` carries no numbers.** No `loanLimit`, no `loanPeriodDays`. Those are
> lending rules, and putting them on the enum would make `member` own policy that belongs
> to `lending`. The tier is a label; `lending` decides what it means. This is a small
> decision that pays off the moment a second module wants to interpret tier differently.

### 5.4 `lending`

```kotlin
data class Loan(
    val id: LoanId,
    val copyId: CopyId,
    val memberId: MemberId,
    val checkedOutAt: Instant,
    val dueOn: LocalDate,
    val returnedAt: Instant?,
) {
    val isReturned: Boolean get() = returnedAt != null

    fun isOverdueOn(today: LocalDate): Boolean = !isReturned && today.isAfter(dueOn)

    fun returnOn(at: Instant): Outcome<LendingError, Loan> =
        if (isReturned) Failure(LendingError.AlreadyReturned(id))
        else Success(copy(returnedAt = at))
}

// lending's OWN view of a member — it does not import member.domain.Member
data class Borrower(val id: MemberId, val tier: MembershipTier)

sealed interface LendingError {
    data class CopyNotFound(val barcode: Barcode) : LendingError
    data class CopyNotAvailable(val barcode: Barcode) : LendingError
    data class MemberNotFound(val id: MemberId) : LendingError
    data class LoanLimitReached(val limit: Int) : LendingError
    data object HasOverdueLoans : LendingError
    data class LoanNotFound(val id: LoanId) : LendingError
    data class AlreadyReturned(val id: LoanId) : LendingError
}
```

**Policies — the OCP core:**

```kotlin
data class BorrowingContext(
    val borrower: Borrower,
    val activeLoans: List<Loan>,
    val today: LocalDate,
)

fun interface BorrowingRule {
    fun check(context: BorrowingContext): Outcome<LendingError, Unit>
}

class LoanLimitRule(private val limits: Map<MembershipTier, Int>) : BorrowingRule
class NoOverdueLoansRule : BorrowingRule

fun interface LoanPeriodPolicy {
    fun dueDateFor(borrower: Borrower, from: LocalDate): LocalDate
}

class TieredLoanPeriodPolicy(private val days: Map<MembershipTier, Long>) : LoanPeriodPolicy
```

The use case injects `List<BorrowingRule>` and folds over it. **Write the ugly version
first** — a `when (borrower.tier)` block with hardcoded numbers — then refactor to rules
and look at the diff. That contrast is the most valuable single exercise in this project.

**Events** (published by `lending`, consumed by `notification`):

```kotlin
data class CopyCheckedOut(val loanId: LoanId, val memberId: MemberId,
                          val copyId: CopyId, val dueOn: LocalDate)

data class CopyReturned(val loanId: LoanId, val memberId: MemberId,
                        val copyId: CopyId, val returnedAt: Instant, val wasOverdue: Boolean)
```

---

## 6. Database schema

Flyway migrations in `src/main/resources/db/migration/`. One table per aggregate. **No
foreign key from `loans` to `copies` or `members`** — those cross module boundaries, and
enforcing them in the database would couple modules through the schema. The application
enforces it.

```sql
-- V1__catalog.sql
CREATE TABLE books (
    id             UUID PRIMARY KEY,
    isbn           VARCHAR(13)  NOT NULL UNIQUE,
    title          VARCHAR(500) NOT NULL,
    author         VARCHAR(300) NOT NULL,
    published_year INT          NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE copies (
    id         UUID PRIMARY KEY,
    book_id    UUID        NOT NULL REFERENCES books(id),
    barcode    VARCHAR(50) NOT NULL UNIQUE,
    status     VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_copies_book_id ON copies(book_id);
CREATE INDEX idx_copies_status  ON copies(status);

-- V2__member.sql
CREATE TABLE members (
    id         UUID PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    email      VARCHAR(320) NOT NULL UNIQUE,
    tier       VARCHAR(20)  NOT NULL,
    joined_on  DATE         NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- V3__lending.sql
CREATE TABLE loans (
    id              UUID PRIMARY KEY,
    copy_id         UUID        NOT NULL,
    member_id       UUID        NOT NULL,
    checked_out_at  TIMESTAMPTZ NOT NULL,
    due_on          DATE        NOT NULL,
    returned_at     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_loans_member_active ON loans(member_id) WHERE returned_at IS NULL;
CREATE UNIQUE INDEX idx_loans_copy_active ON loans(copy_id) WHERE returned_at IS NULL;

-- V4__modulith_event_publication.sql   (copy from Spring Modulith's schema resource)
```

`idx_loans_copy_active` is the real safety net: **a copy can have at most one open loan,
enforced by the database.** Application checks race; a partial unique index does not.

### UUID value classes at the JDBC boundary

`@JvmInline value class LoanId(val value: UUID)` is right for the domain and is exactly
what Spring Data JDBC will not map unaided. Register converters once, in the persistence
adapter, in Phase 1 — before there are four of them:

```kotlin
@Configuration
class JdbcConversionConfig {
    @Bean
    fun jdbcCustomConversions() = JdbcCustomConversions(listOf(
        Converter<LoanId, UUID> { it.value },
        Converter<UUID, LoanId> { LoanId(it) },
        // … one pair per id type
    ))
}
```

Budget an afternoon for this the first time. It is the most predictable friction point in
the whole build.

---

## 7. HTTP API

**Yes — there are creation APIs for books, copies, and members.** Full list, 11 endpoints:

### Catalog

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/books` | Register a book (title-level record) |
| `GET` | `/api/books?query=&page=0&size=20` | List / search by title or author |
| `GET` | `/api/books/{bookId}` | Single book |
| `POST` | `/api/books/{bookId}/copies` | Add a physical copy |
| `GET` | `/api/books/{bookId}/copies` | Copies of a book, with status |

### Members

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/members` | Register a member |
| `GET` | `/api/members/{memberId}` | Single member |
| `GET` | `/api/members?page=0&size=20` | List members |

### Lending

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/loans` | Check out a copy |
| `POST` | `/api/loans/{loanId}/return` | Return a copy |
| `GET` | `/api/loans?memberId=&activeOnly=true&page=0&size=20` | Loans, filterable |
| `GET` | `/api/members/{memberId}/history` | **Borrowing history** — past + current loans for a member (overview of what was borrowed and returned) |

> History is a **read model** in `lending`, not a new aggregate. It reuses the `loans`
> table via a dedicated query port returning DTOs — no aggregate loading for reads.
> `activeOnly=false` on `/api/loans` is the list view; `/history` is the member-centric
> overview (checked out, due, returned, was overdue). That is endpoint #12.

### Examples

```http
POST /api/books
{ "isbn": "9780132350884", "title": "Clean Code",
  "author": "Robert C. Martin", "publishedYear": 2008 }

201 Created
{ "id": "6f1c…", "isbn": "9780132350884", "title": "Clean Code",
  "author": "Robert C. Martin", "publishedYear": 2008 }
```

```http
POST /api/books/6f1c…/copies
{ "barcode": "BIB-000123" }

201 Created
{ "id": "a83d…", "bookId": "6f1c…", "barcode": "BIB-000123", "status": "AVAILABLE" }
```

```http
POST /api/loans
{ "memberId": "22b9…", "barcode": "BIB-000123" }

201 Created
{ "id": "d40a…", "copyId": "a83d…", "memberId": "22b9…",
  "checkedOutAt": "2026-08-18T10:14:02Z", "dueOn": "2026-09-01", "returnedAt": null }
```

### Error responses — RFC 9457 `ProblemDetail`

```http
409 Conflict
{ "type": "https://bibliothek.dev/errors/loan-limit-reached",
  "title": "Loan limit reached",
  "status": 409,
  "detail": "Standard members may hold at most 3 loans",
  "instance": "/api/loans" }
```

| Domain error | HTTP |
| --- | --- |
| `ValidationError`, malformed body | `400` |
| `BookNotFound`, `MemberNotFound`, `CopyNotFound`, `LoanNotFound` | `404` |
| `DuplicateIsbn`, `DuplicateEmail` | `409` |
| `CopyNotAvailable`, `LoanLimitReached`, `HasOverdueLoans`, `AlreadyReturned` | `409` |
| `NotificationError.QueueUnavailable` (only on explicit notify paths — **never** fails checkout) | `503` |

Mapping lives in **one** `@RestControllerAdvice` per module. `ProblemDetail` is built by
`fold`ing the `Outcome` — controllers contain no `try`/`catch` for domain errors.

### DTOs are not domain objects

Every endpoint has its own request/response record in `adapter/in/web/dto/`. **Never
serialise a domain type.** Serialising `Book` directly means your public JSON contract
silently changes the day someone renames a field. Mapping is a hand-written extension
function in `adapter/in/web/`, not in the service — that's finding #4 from your last review
avoided by construction.

---

## 8. Package layout

```
com.bibliothek
├── BibliothekApplication.kt
│
├── lending/
│   ├── LendingApi.kt                    ← named interface; the only thing others see
│   ├── domain/                          ← pure Kotlin. no Spring, no Jakarta, no SQL.
│   │   ├── Loan.kt  Borrower.kt  LendingError.kt  LendingEvents.kt
│   │   └── policy/  BorrowingRule.kt  LoanPeriodPolicy.kt  rules/
│   ├── application/
│   │   ├── port/in/    CheckOutCopyUseCase.kt  ReturnCopyUseCase.kt  LoanQuery.kt
│   │   ├── port/out/   LoadLoan.kt  SaveLoan.kt  ActiveLoansOf.kt
│   │   │               CopyRegistry.kt  MemberDirectory.kt
│   │   └── service/    CheckOutCopyService.kt  (internal)  ReturnCopyService.kt
│   └── adapter/
│       ├── in/web/         LoanController.kt  dto/  LendingExceptionAdvice.kt
│       ├── out/persistence/ LoanRecord.kt  LoanJdbcRepository.kt  LoanMapper.kt
│       └── out/catalog/    CopyRegistryAdapter.kt   ← implements lending's port
│       └── out/member/     MemberDirectoryAdapter.kt   using catalog/member APIs
│
├── catalog/   (same shape)
├── member/    (same shape, thinner)
├── notification/  LoanNotificationListener.kt  ← no domain/; calls shared NotificationGateway
└── shared/    ids/  Outcome.kt  Isbn.kt  Email.kt  notification/  problem/  time/
```

**The dependency rule:** `adapter → application → domain`, and adapters implement the ports
the inner rings declare. `domain` imports nothing but `kotlin.*`, `java.time.*`, and
`shared`. `application` may know `@Transactional` — that's the one deliberate framework
concession.

### Cross-module calls — the DIP payoff

`lending` needs to know whether a copy is available. It does **not** import
`catalog.domain.Copy`. It declares its own port, in its own vocabulary:

```kotlin
// lending/application/port/out/CopyRegistry.kt
interface CopyRegistry {
    fun reserveForLoan(barcode: Barcode): Outcome<LendingError, CopyId>
    fun release(copyId: CopyId)
}
```

`lending/adapter/out/catalog/CopyRegistryAdapter` implements it by calling `CatalogApi`.
If `catalog` becomes its own service later, only that one adapter file changes.

**Synchronous vs. events — the rule of thumb:** if the caller's own invariant depends on
the answer, call it. Otherwise publish an event. `lending` calls `CopyRegistry` (it cannot
create a loan without knowing the copy is free). `lending` publishes `CopyCheckedOut` and
has no idea `notification` is listening — adding a second listener requires zero changes
to `lending`. That's OCP at module scale.

`@ApplicationModuleListener` bundles `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`
+ `@Transactional(REQUIRES_NEW)`, and Modulith's **event publication registry** persists
each publication until its listener completes — a transactional outbox from a dependency.

### Messaging & queue resilience

Two layers, deliberately:

1. **In-process (Modulith JDBC outbox):** `lending` publishes `CopyCheckedOut` /
   `CopyReturned`. The publication row is written in the same transaction as the loan.
   If the JVM dies before the listener runs, Modulith retries. Checkout **never** waits
   on RabbitMQ.
2. **External (RabbitMQ):** the `notification` listener maps the domain event to a
   `NotificationMessage` and calls `NotificationGateway.enqueue`. The RabbitMQ adapter
   lives behind that port.

**What if the queue is broken?**

| Situation | Behaviour |
| --- | --- |
| RabbitMQ down when the Modulith listener runs | Listener fails → Modulith leaves the publication incomplete → automatic retry. Loan already committed. |
| Exhausted / operator-visible failure | Surface as RFC 9457 `ProblemDetail` (`type: …/queue-unavailable`, `status: 503`) on any *synchronous* notification probe; never roll back a completed loan |
| Local / test without a broker | Gateway stub logs; Testcontainers RabbitMQ for adapter tests |

This is the outbox pattern with Zalando-style problem responses on top: durable intent
first, machine-readable failure second.

---

## 9. Architecture rules — enforced by the build

These are tests in `src/test/kotlin/architecture/`. **They are written in Phase 0, against
an empty codebase.** Retrofitting them later means a day of arguing with violations and a
strong pull toward weakening the rules.

| Test | Catches |
| --- | --- |
| `ApplicationModules.of(App::class).verify()` | Any module reaching into another's internals; cycles |
| `domain is free of framework dependencies` | `org.springframework..`, `jakarta..`, `tools.jackson..`, `..adapter..` in `..domain..` |
| `adapters are not referenced by inner rings` | `application` or `domain` importing `..adapter..` |
| `use cases expose exactly one public method` | SRP drift toward god services |
| `service implementations are internal` | Leaking impls past the module boundary (Konsist — ArchUnit can't see Kotlin visibility) |
| `no TODO() or UnsupportedOperationException in overrides` | The classic LSP violation |
| `no domain type is annotated with @Table or @Entity` | Persistence creeping into the domain |
| `Documenter(modules).writeDocumentation()` | Regenerates C4 diagrams into `docs/` each build — diagram drift becomes impossible |

Key example:

```kotlin
@Test
fun `domain is free of framework dependencies`() {
    noClasses().that().resideInAPackage("..domain..")
        .should().dependOnClassesThat().resideInAnyPackage(
            "org.springframework..", "jakarta..", "tools.jackson..",
            "..adapter..", "..application..",
        ).check(classes)
}
```

---

## 10. Testing strategy

The shape matters more than the count. Most tests should be the fast kind.

| Level | Tool | Speed | Coverage target |
| --- | --- | --- | --- |
| Domain | Plain JUnit, **no Spring context** | ms | Every rule, every state transition, every boundary |
| Use case | JUnit + hand-written in-memory fakes | ms | Every use case, happy path + each error |
| Persistence adapter | `@DataJdbcTest` + Testcontainers | seconds | Mapper round-trip, each query |
| Web adapter | `@WebMvcTest` | seconds | Status codes, `ProblemDetail` shape, validation |
| Module | `@ApplicationModuleTest` + Modulith `Scenario` | seconds | Event flows |
| Architecture | ArchUnit + Modulith `verify()` | ms | §9 |
| End-to-end | `@SpringBootTest` + Testcontainers | slow | **Two** happy paths only |

**Conventions, taken from your last review:**

- Testcontainers: use `PostgreSQLContainer` and `RabbitMQContainer`, not `GenericContainer`
  — they have proper wait strategies. Wire once in abstract base classes
  (`PostgresIntegrationTest`, `RabbitIntegrationTest`), not copy-pasted per test class.
- **Never assert on generated IDs.** Capture the ID from the response and use it. No
  `ALTER SEQUENCE … RESTART` in test setup.
- Backtick test names that read as specifications: `` `checkout is rejected when member has an overdue loan` ``.
- Validate at the boundary (`@Valid` on the DTO) **or** in the domain — pick one per rule
  and don't duplicate it in both. Duplicated validation is how the two sources of truth
  silently diverge.
- **No comments explaining *what* the next line does.** Comments explain *why*.

---

## 11. Build plan

| Phase | What | Days | Done when |
| --- | --- | --- | --- |
| **0** | Skeleton + **all arch rules green against empty code**. Gradle, Boot, Modulith, empty module packages, Flyway baseline, Docker Compose with a Postgres healthcheck, Testcontainers base class, CI running `./gradlew check` | 1–2 | An intentionally-added illegal import **fails the build** |
| **1** | `catalog`. Book + Copy, JDBC converters, hand-written mapper, in-memory fakes, 5 endpoints. **Sets the pattern the other modules copy** — take the time here | 2–3 | Domain tests run in <1s with no Spring context |
| **2** | `member`. 3 endpoints. Should feel mechanical — it's rep two of the Phase 1 pattern | 1 | Written mostly by copying Phase 1's shape |
| **3** | `lending`. `Loan`, `CopyRegistry` / `MemberDirectory` ports + adapters, checkout + return, events published. **Write the ugly `when` version of the rules first, then refactor to `BorrowingRule` and read the diff** | 3–4 | A new borrowing rule can be added by creating **one file** |
| **4** | `notification` + history + hardening. Event listener → shared gateway → RabbitMQ adapter, Modulith `Scenario` tests, member history endpoint, generated C4 diagrams into `docs/`, seed data script, ADRs for the real decisions | 1–2 | Someone new can read `docs/` and explain the shape |

**Total: 8–11 focused days.** Phases 0–3 are load-bearing. If time runs short, stop cleanly
after Phase 3 — a finished three-module system beats a half-built four-module one.

### Definition of done, per module

- [ ] Domain tests pass with no Spring context
- [ ] Every use case has a test with in-memory fakes
- [ ] Persistence adapter has a `@DataJdbcTest` round-trip test
- [ ] Controller has a `@WebMvcTest` covering success + each error status
- [ ] All §9 architecture tests still green
- [ ] Named interface exposes the minimum — ideally one type
- [ ] ktlint + detekt clean
- [ ] This document updated if anything here changed

---

## 12. Known friction — plan for it

| Thing | Reality | Response |
| --- | --- | --- |
| Kotlin has no `package-info.java` | Modulith's `@ApplicationModule` / `@NamedInterface` metadata is package-level | Keep a tiny `src/main/java` tree containing *only* `package-info.java` files. Gradle compiles both fine. Check your Modulith version first — type-level support has been improving |
| Value classes at the JDBC boundary | Won't map unaided | §6 converters, done in Phase 1 |
| Boot 4's module split | Things transitive in Boot 3 now need explicit starters — Flyway is the usual casualty | Expect one round of "why is this bean missing" in Phase 0 |
| Mapping boilerplate domain ↔ persistence | Real; it's the price of a pure domain | Accept it. **Do not reach for MapStruct on domain objects** — generated mappers want open classes and no-arg constructors, reintroducing exactly what Data JDBC was chosen to avoid |
| Async events are harder to debug | Also real | The event publication table is queryable; add `spring-modulith-starter-insight` in Phase 4 for module-level traces |
| Modulith 2.1 / Boot 4.1 pairing | The 2.x line targets Boot 4.x | Generate the Phase 0 skeleton from start.spring.io and let the BOM resolve versions — don't pin by hand |

---

## 13. Decisions on the record

Write these as ADRs in `docs/adr/` during Phase 4. They exist so future-you doesn't
"fix" them.

1. **Spring Data JDBC over JPA** — §4. Keeps the domain free of persistence concerns.
2. **Modular monolith over microservices** — one deployment, enforced boundaries, cheap extraction later if ever needed.
3. **`notification` has no domain layer; contracts live in `shared`** — no business rules in the listener; gateway is reusable without reverse deps.
4. **Tier carries no numbers** — `member` owns identity, `lending` owns policy.
5. **`Outcome` over exceptions for domain errors** — failure is part of the signature.
6. **No FKs across module boundaries** — schema-level coupling is still coupling.
7. **Spring `ProblemDetail` (RFC 9457) over Zalando `problem-spring-web`** — same RFC, first-party; Zalando library is maintenance-mode. Conventions (typed URIs) stay.
8. **Modulith outbox + RabbitMQ for notifications** — durable in-process events; external queue for delivery; queue outage never fails a loan; `503` ProblemDetail when a sync path must report it.
9. **Open API, no auth** — authentication/authorisation are out of scope; springdoc exposes everything.
10. **Borrowing history is a lending read model** — `GET /api/members/{id}/history`, not a new aggregate.

---

*Bibliothek — small on purpose. Four aggregates, twelve endpoints, three rules that argue
with each other. Everything else is out of scope.*
