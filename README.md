# High-Concurrency Event Ticketing API

A concurrency-aware ticket booking backend designed to handle high-contention seat reservations during flash sales while maintaining transactional consistency, preventing double bookings, and ensuring reliable booking confirmation under concurrent requests.

Built using **Java 21, Spring Boot 4.1.1, PostgreSQL 16, and Redis 7**, this project explores real-world backend engineering challenges involving database concurrency control, distributed coordination, idempotent operations, automated resource management, caching, authentication, and integration testing.

---

## Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Booking Lifecycle](#booking-lifecycle)
- [Concurrency Control](#concurrency-control)
- [Redis Integration](#redis-integration)
- [Idempotency Engine](#idempotency-engine)
- [Automated Hold Expiration](#automated-hold-expiration)
- [Authentication and Authorization](#authentication-and-authorization)
- [Caching Strategy](#caching-strategy)
- [Observability and Logging](#observability-and-logging)
- [Database Design](#database-design)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Testing Strategy](#testing-strategy)
- [Concurrency Testing](#concurrency-testing)
- [CI/CD](#cicd)
- [Design Decisions and Trade-offs](#design-decisions-and-trade-offs)
- [Future Improvements](#future-improvements)
- [Author](#author)

---

## Overview

Traditional ticket booking systems face a significant challenge during high-demand events and flash sales: multiple users attempting to purchase the same seat simultaneously.

Without proper concurrency control, simultaneous booking requests can result in:

- Double bookings.
- Race conditions during seat reservation.
- Inconsistent database state.
- Duplicate booking operations caused by network retries.
- Seats remaining unavailable after users abandon checkout.
- Excessive database load during read-heavy traffic.

This project addresses these challenges through a concurrency-aware booking architecture using PostgreSQL row-level locking, Redis-based fast-path coordination, transactional state management, and idempotent confirmation operations.

### The Flash Sale Scenario

Consider an event with a limited number of seats.

During a flash sale, 500 users attempt to reserve the exact same seat simultaneously.

The system must ensure that:

1. Only one request successfully acquires the seat.
2. Competing requests are rejected gracefully.
3. The database maintains a consistent booking state.
4. Abandoned reservations automatically expire.
5. Retrying a confirmation request does not create duplicate bookings.
6. Database-level consistency is maintained even when requests compete simultaneously.

The architecture uses Redis to reject duplicate reservation attempts early and PostgreSQL as the authoritative source of truth for seat availability and booking state.

---

## Key Features

### 1. High-Concurrency Booking Engine

- Handles competing booking requests targeting the same seat.
- Uses PostgreSQL pessimistic row-level locking through JPA `@Lock(PESSIMISTIC_WRITE)`.
- Implements optimistic locking using JPA `@Version`.
- Uses Redis atomic key insertion with expiration as a fast-path duplicate-request guard.
- Uses transactional state transitions to protect booking consistency.

### 2. Idempotent Booking Confirmation

- Requires an `Idempotency-Key` HTTP header during booking confirmation.
- Prevents duplicate confirmation processing when clients retry requests.
- Returns the previously stored confirmation result for an identical retry.
- Uses database uniqueness constraints to protect idempotency records.

### 3. Automated Seat-Hold Expiration

- Holds seats for a 15-minute reservation window.
- Uses a Spring `@Scheduled` background worker to identify expired holds.
- Runs the cleanup process every 60 seconds.
- Automatically releases abandoned seats back to the `AVAILABLE` state.
- Removes corresponding Redis hold keys during cleanup.

### 4. Redis Integration

- Uses Redis for fast-path reservation coordination.
- Implements Redis cache-aside for frequently accessed event data.
- Applies a 30-second TTL to configured event caches.
- Reduces repeated database reads during high-traffic browsing.

### 5. Stateless JWT Authentication

- Implements JWT-based authentication using Spring Security.
- Uses BCrypt password hashing.
- Supports role-based access control.
- Restricts event creation to users with the `ADMIN` role.
- Keeps public event browsing endpoints accessible without authentication.

### 6. Concurrency Testing

- Uses JUnit 5 and Testcontainers.
- Runs integration tests against PostgreSQL and Redis containers.
- Uses Java `ExecutorService` and `CountDownLatch` to coordinate concurrent requests.
- Simulates 500 simultaneous attempts to reserve the same seat.
- Validates that exactly one request succeeds while the remaining requests are rejected.

### 7. Centralized Observability

- Uses Log4j2 for application logging.
- Implements a custom `OncePerRequestFilter` for correlation ID generation.
- Injects UUID-based correlation IDs into Log4j2 Mapped Diagnostic Context (MDC).
- Enables request-level log tracing across application components.

---

## Tech Stack

| Category | Technologies |
|:---|:---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring Web |
| ORM | Spring Data JPA, Hibernate |
| Database | PostgreSQL 16 |
| Caching and Coordination | Redis 7, Spring Data Redis |
| Security | Spring Security 7, JWT |
| Validation | Spring Boot Validation |
| Database Migration | Flyway |
| Testing | JUnit 5, Mockito, Testcontainers |
| Containerization | Docker, Docker Compose |
| Build Tool | Maven |
| Logging | Log4j2 |
| CI/CD | GitHub Actions |
| Development Tools | Git, GitHub, Postman |

---

## System Architecture

The application follows a layered Spring Boot architecture that separates HTTP request handling, business logic, persistence, caching, and background processing.

### High-Level Architecture

```text
                         Client Applications
                                  |
                                  v
                         Spring Boot REST API
                                  |
                                  v
                       Spring Security Filter Chain
                                  |
                         JWT Authentication
                                  |
                                  v
                             Controllers
                                  |
                                  v
                              Services
                                  |
                  +---------------+---------------+
                  |                               |
                  v                               v
             Redis Layer                    PostgreSQL
                  |                               |
          +-------+--------+             +--------+--------+
          |                |             |                 |
          v                v             v                 v
     Fast-Path         Cache-Aside     JPA Repositories  Transactions
   Coordination       Event Data       and Row Locks     and Constraints
          |                                       
          v
     Seat Hold Keys

                  Background Processing
                           |
                           v
                    Scheduled Reaper
                           |
                           v
                  Expired Hold Cleanup

                  Observability Layer
                           |
                           v
                  Log4j2 + MDC Context
                           |
                           v
                  Correlation ID Tracing
```

### Component Responsibilities

| Component | Responsibility |
|:---|:---|
| Spring Web | Exposes REST API endpoints. |
| Spring Security | Handles authentication and authorization. |
| JWT Filter | Validates incoming authentication tokens. |
| Controllers | Handles HTTP requests and responses. |
| Services | Implements booking, confirmation, and business logic. |
| Spring Data JPA | Handles database persistence and entity operations. |
| PostgreSQL | Maintains authoritative booking and seat state. |
| Redis | Handles fast-path coordination and caching. |
| Flyway | Manages version-controlled database migrations. |
| Scheduled Worker | Releases expired seat holds. |
| Log4j2 | Handles application logging. |
| MDC Filter | Adds correlation IDs to request logs. |

---

## Booking Lifecycle

Each seat follows a controlled state transition throughout its booking lifecycle.

### Seat State Diagram

```text
                 +-----------+
                 | AVAILABLE |
                 +-----------+
                       |
                       | Successful Hold
                       v
                  +--------+
                  |  HELD  |
                  +--------+
                       |
              +--------+--------+
              |                 |
              |                 |
       Confirmation         Hold Expiration
              |                 |
              v                 v
          +--------+       +-----------+
          | BOOKED |       | AVAILABLE |
          +--------+       +-----------+
              |
              |
         Cancellation
              |
              v
         +-----------+
         | AVAILABLE |
         +-----------+
```

### Seat States

| State | Description |
|:---|:---|
| `AVAILABLE` | Seat is available for reservation. |
| `HELD` | Seat is temporarily reserved by a user. |
| `BOOKED` | Seat has been successfully confirmed. |

### State Transition Rules

- `AVAILABLE` → `HELD`: A user successfully reserves the seat.
- `HELD` → `BOOKED`: The user successfully confirms the booking.
- `HELD` → `AVAILABLE`: The hold expires or the user cancels the reservation.
- `BOOKED` → `AVAILABLE`: A confirmed booking is cancelled according to the cancellation workflow.

Invalid state transitions are rejected by the business logic.

The database also enforces allowed seat status values through schema-level constraints.

---

## Concurrency Control

Concurrency control is the core of this application.

When multiple requests attempt to modify the same seat simultaneously, the system must prevent race conditions and maintain a consistent database state.

This implementation uses Redis-based fast-path coordination and PostgreSQL locking.

### 1. Redis Fast-Path Coordination

Before performing the database operation, the application uses Redis to identify competing reservation attempts.

A Redis key follows the naming convention:

```text
seat:hold:{seatId}
```

The application uses atomic key insertion with expiration, equivalent to Redis `SET NX EX` behavior.

The expected behavior is:

- If the key does not exist, the request can acquire the key and proceed to database validation.
- If the key already exists, the request is rejected early.
- The key has an expiration time to prevent abandoned coordination keys from remaining indefinitely.

This reduces unnecessary database contention during high-demand booking scenarios.

**Important:** Redis is not the ultimate authority for seat ownership. PostgreSQL remains responsible for enforcing the actual booking state and consistency.

### 2. Pessimistic Locking

The application uses JPA pessimistic locking through:

`@Lock(LockModeType.PESSIMISTIC_WRITE)`

The repository implementation follows this pattern:

```java
import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId")
    Optional<Seat> findByIdForUpdate(@Param("seatId") Long seatId);

}
```

When a transaction fetches a seat using this lock, PostgreSQL acquires a row-level write lock.

Competing transactions attempting to modify the same row must wait until the lock is released or the transaction completes.

The database transaction validates the current seat state before performing the transition.

### 3. Optimistic Locking

The `Seat` entity also contains a version field:

```java
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

@Entity
public class Seat {

    @Id
    private Long id;

    @Version
    private Long version;

}
```

Hibernate uses this version to detect conflicting updates.

When an entity is updated, the version is checked. If another transaction has modified the row since it was read, the stale update is detected and an optimistic locking exception can occur.

This provides an additional concurrency protection mechanism.

### Pessimistic vs. Optimistic Locking

| Aspect | Pessimistic Locking | Optimistic Locking |
|:---|:---|:---|
| Approach | Acquires a database lock before modification. | Detects conflicting updates during modification. |
| Mechanism | `PESSIMISTIC_WRITE` | `@Version` |
| Contention | Competing transactions wait. | Conflicting transactions may fail. |
| Best Suited For | Highly contested shared resources. | Lower-contention updates. |
| Main Trade-off | Lock contention and potential deadlocks. | Retry or conflict handling is required. |

### Why Use Both?

The booking operation uses pessimistic locking as its primary protection for highly contested seat reservations.

Optimistic locking provides additional protection against stale updates in workflows that use version-aware entity updates.

Redis helps reduce unnecessary competing requests, but database locking and transactional checks ensure that application correctness does not depend solely on Redis.

---

## Redis Integration

Redis serves two distinct purposes in this application.

### 1. Fast-Path Reservation Coordination

Redis is used to reject duplicate reservation attempts before they create unnecessary database contention.

Example key:

```text
seat:hold:{seatId}
```

The key uses an expiration mechanism so abandoned coordination entries do not remain indefinitely.

### 2. Event Data Caching

The application implements a cache-aside strategy for frequently accessed event information.

The configured cache TTL is 30 seconds.

### Cache-Aside Read Flow

```text
              Client Request
                   |
                   v
              Event Service
                   |
                   v
              Check Redis
                   |
            +------+------+
            |             |
         Cache Hit     Cache Miss
            |             |
            v             v
       Return Data   Query PostgreSQL
                          |
                          v
                     Store in Redis
                          |
                          v
                       Return Data
```

Caching reduces repeated database queries for frequently accessed event information.

Configured cache operations include:

- Event listings.
- Event details.
- Seat availability information for event browsing.

The cache is used for read optimization and is not treated as the authoritative source for booking decisions.

Booking operations always validate the current database state.

---

## Idempotency Engine

A client may successfully send a booking confirmation request while losing its network connection before receiving the response.

The client cannot know whether the original request succeeded.

Retrying without idempotency protection could result in duplicate processing.

This application addresses that problem using an `Idempotency-Key` HTTP header.

### Confirmation Request

```http
POST /api/bookings/{id}/confirm
Authorization: Bearer <JWT>
Idempotency-Key: unique-request-key
Content-Type: application/json
```

### Confirmation Flow

```text
                Confirmation Request
                          |
                          v
                Read Idempotency Key
                          |
                          v
                Check Existing Result
                          |
                +---------+---------+
                |                   |
          Existing Key         New Key
                |                   |
                v                   v
         Return Stored       Validate Booking
         Response                  |
                                   v
                            Validate Seat Hold
                                   |
                                   v
                            Simulate Payment
                                   |
                                   v
                           Update Booking State
                                   |
                                   v
                         Persist Confirmation
                                   |
                                   v
                            Return Response
```

### Expected Behavior

**First Request:**

- The application receives a unique idempotency key.
- The key is checked against existing records.
- The booking and hold state are validated.
- The payment operation is simulated.
- The booking is confirmed.
- The confirmation result is stored.

**Retry Request:**

- The application receives the same idempotency key.
- The existing result is identified.
- The original confirmation response is returned.
- The booking is not confirmed again.

A database uniqueness constraint protects the idempotency key against duplicate insertion.

This design makes confirmation operations resilient to network retries and repeated client requests.

**Note:** Payment processing in this project is simulated. It does not integrate with an external payment gateway.

---

## Automated Hold Expiration

A seat should not remain unavailable indefinitely when a user abandons the checkout process.

The application implements an automated expiration mechanism using Spring's `@Scheduled` functionality.

### Hold Configuration

| Setting | Value |
|:---|:---|
| Hold Duration | 15 minutes |
| Reaper Execution Interval | 60 seconds |
| Seat State After Expiration | `AVAILABLE` |
| Background Processing | Spring `@Scheduled` |

### Cleanup Workflow

```text
                 Scheduled Worker
                        |
                        v
                Execute Every 60s
                        |
                        v
              Query Expired Holds
                        |
                        v
             Identify HELD Seats
                        |
                        v
             Validate Expiration Time
                        |
                        v
              Release Expired Holds
                        |
                        v
              Update Seat to AVAILABLE
                        |
                        v
              Remove Redis Hold Key
```

The worker identifies held seats whose expiration timestamps have passed.

Expired holds are released, and the corresponding Redis coordination keys are removed.

The cleanup operation is performed through database state transitions so that expired reservations can return to the available seat pool.

---

## Authentication and Authorization

The application implements stateless authentication using Spring Security and JSON Web Tokens (JWT).

### Authentication Flow

```text
                  User Registration
                        |
                        v
                BCrypt Password Hash
                        |
                        v
                 Store User in DB


                    User Login
                        |
                        v
                Validate Credentials
                        |
                        v
                    Generate JWT
                        |
                        v
                 Return Access Token
                        |
                        v
               Subsequent API Requests
                        |
                        v
                  JWT Filter
                        |
                        v
               Validate JWT Token
                        |
                        v
             Set Authentication Context
                        |
                        v
                  Access Endpoint
```

### Security Implementation

- Stateless session management.
- JWT-based authentication.
- BCrypt password hashing.
- Custom JWT authentication filter.
- Role-based access control.
- Public and authenticated endpoint separation.

### Authorization Rules

| Operation | Access |
|:---|:---|
| User Registration | Public |
| User Login | Public |
| View Events | Public |
| View Event Details | Public |
| View Event Seats | Public |
| Create Event | ADMIN |
| Hold Seat | Authenticated User |
| Confirm Booking | Authenticated User |
| Cancel Booking | Authenticated User |
| View My Tickets | Authenticated User |

Event creation is restricted to users with the `ROLE_ADMIN` authority.

---

## Caching Strategy

The application uses Redis cache-aside to reduce repeated database reads.

### Cache Configuration

| Property | Configuration |
|:---|:---|
| Cache Provider | Redis |
| Caching Strategy | Cache-Aside |
| Cache TTL | 30 seconds |
| Serialization | JSON |
| Primary Data Source | PostgreSQL |

### Implementation

Redis caching is configured through Spring's caching abstraction.

Event service operations use annotations such as:

```java
import org.springframework.cache.annotation.Cacheable;

import org.springframework.stereotype.Service;

@Service
public class EventService {

    @Cacheable(value = "events", key = "#eventId")
    public Event getEventById(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Event not found"));
    }

}
```

The first request retrieves the event from PostgreSQL and stores it in Redis.

Subsequent requests can retrieve the cached event until the entry expires or is invalidated according to the cache configuration.

### Cache Consistency

Redis caching is intended for read-heavy event browsing.

Seat booking and confirmation decisions do not rely on cached seat availability.

The application validates the actual seat state against PostgreSQL before committing booking operations.

This separation prevents cached read data from becoming the authority for concurrent booking decisions.

---

## Observability and Logging

The application uses Log4j2 for application logging.

A custom `OncePerRequestFilter` generates a UUID correlation ID for incoming HTTP requests.

The correlation ID is stored in Log4j2's Mapped Diagnostic Context (MDC).

### Request Tracing Flow

```text
              Incoming HTTP Request
                        |
                        v
                  MDC Filter
                        |
                        v
             Generate Correlation UUID
                        |
                        v
               Store ID in MDC
                        |
                        v
              Process Application
                        |
                        v
              Log4j2 Log Statements
                        |
                        v
             Correlation ID in Logs
```

This helps identify and trace all log entries associated with a particular HTTP request.

It is especially useful for debugging concurrent booking requests, authentication failures, and booking state transitions.

The logging configuration also uses MDC context to associate booking lifecycle logs with their originating request.

---

## Database Design

The application uses PostgreSQL 16 as its primary relational database.

### Main Entities

| Entity | Responsibility |
|:---|:---|
| User | Stores user information, credentials, and roles. |
| Event | Represents an event and its details. |
| Seat | Represents an individual seat and its availability state. |
| Booking | Represents a user's seat reservation and confirmation. |

### Entity Relationships

The data model includes the following relationships:

- A user can have multiple bookings.
- An event can contain multiple seats.
- A seat belongs to an event.
- A booking references a user and a seat.

### Database Integrity

Flyway manages database schema migrations.

The initial migration defines the relational schema and database-level constraints.

An example seat status constraint is:

```sql
ALTER TABLE seats
ADD CONSTRAINT chk_seat_status
CHECK (status IN ('AVAILABLE', 'HELD', 'BOOKED'));
```

The database also uses:

- Primary key constraints.
- Foreign key constraints.
- Unique constraints where required.
- Indexed idempotency keys.
- Version-based optimistic locking.
- Transactional database operations.

These constraints provide an additional layer of protection beyond application-level validation.

### Database Migrations

Migration files are stored under:

```text
src/main/resources/db/migration/
```

Example migration:

```text
V1__init_schema.sql
```

Flyway automatically applies pending migrations during application startup.

---

## API Reference

The following table documents the main API operations exposed by the application.

### Authentication and Users

| Method | Endpoint | Description | Access |
|:---|:---|:---|:---|
| POST | `/api/auth/register` | Register a new user. | Public |
| POST | `/api/auth/login` | Authenticate and retrieve a JWT. | Public |

### Events and Seats

| Method | Endpoint | Description | Access |
|:---|:---|:---|:---|
| POST | `/api/events` | Create a new event. | ADMIN |
| GET | `/api/events` | Retrieve event listings. | Public |
| GET | `/api/events/{id}` | Retrieve event details. | Public |
| GET | `/api/events/{id}/seats` | Retrieve seats associated with an event. | Public |

### Booking Engine

| Method | Endpoint | Description | Access |
|:---|:---|:---|:---|
| POST | `/api/bookings/hold` | Temporarily reserve a seat for 15 minutes. | Authenticated |
| POST | `/api/bookings/{id}/confirm` | Confirm a booking using an idempotency key. | Authenticated |
| POST | `/api/bookings/{id}/cancel` | Cancel a booking and release its seat. | Authenticated |
| GET | `/api/bookings/my-tickets` | Retrieve the authenticated user's bookings. | Authenticated |

### Idempotency Header

Booking confirmation requires the following HTTP header:

```http
Idempotency-Key: <unique-request-key>
```

The same key is used when retrying the exact same confirmation operation.

---

## Project Structure

The application follows a layered package structure to separate responsibilities.

```text
src/
├── main/
│   ├── java/
│   │   └── com/
│   │       └── barath/
│   │           └── ticketingapi/
│   │               ├── config/
│   │               │   ├── RedisCacheConfig
│   │               │   └── SecurityConfig
│   │               │
│   │               ├── controller/
│   │               │   ├── AuthController
│   │               │   ├── EventController
│   │               │   └── BookingController
│   │               │
│   │               ├── filter/
│   │               │   ├── JwtAuthenticationFilter
│   │               │   └── CorrelationIdFilter
│   │               │
│   │               ├── model/
│   │               │   ├── User
│   │               │   ├── Event
│   │               │   ├── Seat
│   │               │   └── Booking
│   │               │
│   │               ├── repository/
│   │               │   ├── UserRepository
│   │               │   ├── EventRepository
│   │               │   ├── SeatRepository
│   │               │   └── BookingRepository
│   │               │
│   │               ├── service/
│   │               │   ├── AuthService
│   │               │   ├── EventService
│   │               │   ├── BookingService
│   │               │   └── HoldCleanupWorker
│   │               │
│   │               └── TicketingApiApplication
│   │
│   └── resources/
│       ├── db/
│       │   └── migration/
│       │       └── V1__init_schema.sql
│       │
│       └── application.yml
│
└── test/
    └── java/
        └── com/
            └── barath/
                └── ticketingapi/
                    ├── integration/
                    └── service/
```

*Note: The tree illustrates the logical organization of the main components. Actual filenames and package paths may vary with the implementation.*

---

## Getting Started

### Prerequisites

Ensure the following tools are installed:

- Java 21 JDK.
- Docker and Docker Compose.
- Git.
- Maven or the included Maven Wrapper.

### 1. Clone the Repository

```bash
git clone <YOUR_REPOSITORY_URL>
```

Navigate to the project directory:

```bash
cd TicketingApi
```

### 2. Start Infrastructure

Start PostgreSQL 16 and Redis 7 using Docker Compose:

```bash
docker compose up -d
```

Verify the running containers:

```bash
docker compose ps
```

The project uses Spring Boot Docker Compose integration for local infrastructure configuration.

### 3. Run the Application

Using the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

On Windows:

```bash
mvnw.cmd spring-boot:run
```

The API will be available at:

```text
http://localhost:8080
```

### 4. Stop Infrastructure

To stop the containers:

```bash
docker compose down
```

To stop containers and remove their associated volumes:

```bash
docker compose down -v
```

**Warning:** Removing volumes deletes persisted local database data.

---

## Configuration

Application configuration is maintained in:

```text
src/main/resources/application.yml
```

The application requires configuration for:

- PostgreSQL datasource.
- Redis connection.
- JPA and Hibernate.
- Flyway migrations.
- JWT authentication.
- Redis caching.
- Logging.
- Scheduled hold cleanup.

### Example Configuration Structure

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}

  jpa:
    hibernate:
      ddl-auto: validate

  flyway:
    enabled: true
```

Use environment variables or an appropriate local configuration file for actual credentials and connection values.

Do not commit production credentials, JWT secrets, or database passwords to GitHub.

---

## Testing Strategy

The application uses JUnit 5, Mockito, and Testcontainers to validate business logic, integration behavior, and database consistency.

### Testing Components

| Technology | Purpose |
|:---|:---|
| JUnit 5 | Unit and integration testing. |
| Mockito | Mocking dependencies during isolated unit tests. |
| Testcontainers | Provisioning isolated PostgreSQL and Redis instances. |
| ExecutorService | Executing concurrent booking requests. |
| CountDownLatch | Synchronizing concurrent test execution. |

### Testcontainers Integration

Instead of relying only on in-memory database testing, the integration suite provisions PostgreSQL and Redis containers.

This provides a more representative environment for validating:

- PostgreSQL locking behavior.
- Transactional state changes.
- Database constraints.
- Redis coordination.
- Booking consistency.
- Concurrent request handling.

The containers are created for the test execution and cleaned up afterward.

### Run the Test Suite

Execute:

```bash
./mvnw clean verify
```

On Windows:

```bash
mvnw.cmd clean verify
```

---

## Concurrency Testing

One of the most important tests in this project simulates a flash sale where 500 concurrent requests attempt to hold the same seat.

### Test Design

The test uses:

- Java `ExecutorService` for concurrent execution.
- `CountDownLatch` to synchronize the start of requests.
- JUnit 5 for test execution and assertions.
- Testcontainers for isolated PostgreSQL and Redis infrastructure.

### Execution Flow

```text
                 Test Initialization
                         |
                         v
             Start PostgreSQL Container
                         |
                         v
                Start Redis Container
                         |
                         v
               Prepare User and Event
                         |
                         v
                Create Available Seat
                         |
                         v
              Initialize ExecutorService
                         |
                         v
                 Create 500 Tasks
                         |
                         v
                CountDownLatch Sync
                         |
                         v
              Release Concurrent Tasks
                         |
                         v
                Execute Hold Requests
                         |
                         v
                Collect API Responses
                         |
                         v
                Verify Database State
```

### Expected Test Results

| Metric | Expected Result |
|:---|---:|
| Concurrent Requests | 500 |
| Successful Seat Holds | 1 |
| Rejected Requests | 499 |
| Final Seat State | `HELD` |
| Duplicate Successful Holds | 0 |

The test validates that competing requests cannot successfully reserve the same seat.

The objective is not simply to test whether the API responds under concurrent traffic, but to verify that the database maintains the correct state despite simultaneous booking attempts.

---

## CI/CD

The project integrates GitHub Actions to automate build and test execution.

The CI workflow runs Maven validation and the integration test suite whenever the configured GitHub events occur.

### CI Pipeline

```text
                GitHub Push / Pull Request
                           |
                           v
                    GitHub Actions
                           |
                           v
                    Ubuntu Runner
                           |
                           v
                     Setup Java 21
                           |
                           v
                    Maven Build
                           |
                           v
                    Docker Runtime
                           |
                           v
                      Testcontainers
                           |
                           v
                 PostgreSQL and Redis
                           |
                           v
                   Integration Tests
                           |
                           v
                 Concurrency Validation
                           |
                           v
                   Build Verification
```

### Pipeline Responsibilities

- Set up the Java 21 environment.
- Execute the Maven build.
- Run automated unit tests.
- Provision Testcontainers infrastructure.
- Execute integration tests.
- Validate booking concurrency behavior.
- Report build and test results.

The pipeline helps catch regressions before changes are merged into the main branch.

---

## Design Decisions and Trade-offs

### 1. Why PostgreSQL Instead of an In-Memory Database?

Booking operations require reliable transactional behavior and row-level concurrency control.

PostgreSQL provides relational integrity, ACID transactions, row-level locking, and database constraints needed for the booking workflow.

### 2. Why Redis Alongside PostgreSQL?

Redis provides a fast in-memory layer for duplicate-request coordination and frequently accessed event data.

Using Redis reduces unnecessary database contention and repeated read queries.

However, PostgreSQL remains the authoritative source for seat availability and booking consistency.

### 3. Why Pessimistic Locking?

Seat booking is a high-contention operation.

Pessimistic locking is appropriate when multiple requests are likely to compete for the same resource because it prevents concurrent transactions from simultaneously modifying the locked seat row.

### 4. Why Optimistic Locking?

Optimistic locking detects stale entity updates through the version field.

It provides additional protection when concurrent modifications occur without requiring every operation to acquire an exclusive lock upfront.

### 5. Why Idempotency Keys?

Network failures and client retries are common in distributed applications.

Idempotency allows a client to retry an operation without accidentally processing the same confirmation multiple times.

### 6. Why Automated Hold Cleanup?

Temporary reservations should not permanently consume inventory when users abandon checkout.

A scheduled worker makes abandoned reservations recoverable without requiring manual intervention.

### 7. Why Testcontainers?

In-memory mocks cannot fully reproduce PostgreSQL row-level locking and Redis behavior.

Testcontainers makes it possible to validate concurrency and integration behavior using actual infrastructure dependencies.

---

## Future Improvements

Potential areas for further development include:

- Integration with a real payment gateway.
- Distributed background processing using message brokers.
- Outbox pattern for reliable event publishing.
- Dead-letter queue handling for failed asynchronous operations.
- Prometheus and Grafana metrics.
- Distributed tracing using OpenTelemetry.
- Load testing with dedicated performance testing tools.
- Multi-instance deployment and horizontal scaling.
- Advanced cache invalidation and consistency strategies.
- Rate limiting and API abuse protection.

---

## Author

**Barath Jogi**

Java Backend Developer

- LinkedIn: [linkedin.com/in/barath-jogi](https://www.linkedin.com/in/barath-jogi/)
- GitHub: [github.com/BarathJogi](https://github.com/BarathJogi)
- Email: barathjogi.bm@gmail.com

---

*Built to explore real-world backend engineering challenges involving concurrency control, transactional consistency, idempotency, caching, and reliable REST API design.*