# Spring Boot 4 - Virtual Threads, Structured Concurrency & Scoped Values Demo

[![Java CI with Maven](https://github.com/hendisantika/spring-boot-vt-structured-concurrency-scoped-values/actions/workflows/maven.yml/badge.svg)](https://github.com/hendisantika/spring-boot-vt-structured-concurrency-scoped-values/actions/workflows/maven.yml)
![Java](https://img.shields.io/badge/Java-25-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.2-6DB33F?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-9.5.0-4479A1?logo=mysql&logoColor=white)
![Dependabot](https://img.shields.io/badge/Dependabot-enabled-025E8C?logo=dependabot)

A comprehensive demonstration of JDK 25's modern concurrency features with Spring Boot 4 and MySQL 9.

## Features Demonstrated

### 1. Virtual Threads (JEP 444)

- Enabled via `spring.threads.virtual.enabled=true`
- Tomcat handles all HTTP requests on virtual threads
- Lightweight threads ideal for I/O-bound operations

### 2. Structured Concurrency (JEP 505 - Fifth Preview)

> Still a **preview** API in JDK 25, so the project is compiled, tested and run with `--enable-preview`
> (already configured in `pom.xml` for the compiler, Surefire and `spring-boot:run`).

JDK 25 introduces a redesigned API with the Joiner pattern:

- **StructuredTaskScope.open()**: Default scope - all subtasks must succeed, fails fast on first error
- **Joiner.anySuccessfulResultOrThrow()**: First successful result wins, cancels remaining tasks
- **Joiner.allSuccessfulOrThrow()**: Collects all successful results as a stream
- `join()` now returns results directly and throws on failure (no separate `throwIfFailed()`)
- Automatic cleanup of child threads when parent completes

### 3. Scoped Values (JEP 506 - Final)

Scoped Values are a **final** feature in JDK 25. The API uses `ScopedValue.where().run()` pattern:

- Request context automatically propagated to child threads
- Immutable, inherited values across virtual thread hierarchies
- Better alternative to ThreadLocal for virtual threads

## Technology Stack

- **Java**: 25 (LTS) - enforced by `maven-enforcer-plugin` (`requireJavaVersion [25,)`)
- **Spring Boot**: 4.1.2
- **Database**: MySQL 9.5.0 (Docker Compose), H2 in-memory for tests
- **Build Tool**: Maven 3.9+ (Maven Wrapper included)
- **CI/CD**: GitHub Actions (Temurin JDK 25) + Dependabot

## Project Structure

```
src/main/java/id/my/hendisantika/springbootvtstructuredconcurrencyscopedvalues/
├── config/
│   ├── DataInitializer.java        # Sample data loader
│   ├── GlobalExceptionHandler.java # Exception handling
│   └── RequestContextFilter.java   # ScopedValue binding filter (JDK 25 API)
├── context/
│   ├── RequestContext.java         # Request context record
│   └── ScopedValues.java           # ScopedValue definitions
├── controller/
│   ├── CustomerController.java
│   ├── DemoController.java         # Feature demonstration endpoints (JDK 25 API)
│   ├── OrderController.java
│   └── ProductController.java
├── dto/
│   ├── CustomerDashboard.java
│   ├── OrderRequest.java
│   └── OrderResponse.java
├── entity/
│   ├── Customer.java
│   ├── Order.java
│   ├── OrderItem.java
│   └── Product.java
├── repository/
│   ├── CustomerRepository.java
│   ├── OrderItemRepository.java
│   ├── OrderRepository.java
│   └── ProductRepository.java
└── service/
    ├── CustomerService.java        # StructuredTaskScope.open() example
    ├── InventoryCheckService.java  # Joiner.anySuccessfulResultOrThrow() example
    ├── OrderService.java           # Parallel validation example
    └── ProductService.java
```

## Getting Started

### Prerequisites

- JDK 25 (GA / LTS, e.g. [Eclipse Temurin 25](https://adoptium.net/))
  - With [SDKMAN!](https://sdkman.io/): `sdk env install` (uses the bundled `.sdkmanrc`, `java=25-tem`)
- Docker & Docker Compose (only needed to run the app against MySQL)
- Maven 3.9+ (or just use the bundled `./mvnw`)

> The build fails fast with a clear message if it is not running on JDK 25+.

### Running the Application

1. **Start MySQL using Docker Compose:**
   ```bash
   docker compose up -d
   ```

2. **Run the Spring Boot application:**
   ```bash
   ./mvnw spring-boot:run
   ```

   > `spring-boot-docker-compose` is on the classpath, so `./mvnw spring-boot:run` will also start
   > `compose.yaml` automatically if MySQL is not already running.

3. **Access the API:**
    - Base URL: `http://localhost:8080`
    - Demo endpoints: `http://localhost:8080/api/demo/*`

### Building & Testing

```bash
# Compile, run tests and package (same command CI uses)
./mvnw -B verify
```

Tests use an **in-memory H2 database** (MySQL compatibility mode) configured in
`src/test/resources/application.properties`, so no MySQL or Docker is required to run them.
Docker Compose integration is disabled during tests.

## API Endpoints

### Demo Endpoints (Feature Showcase)

| Endpoint                            | Description                                     |
|-------------------------------------|-------------------------------------------------|
| `GET /api/demo/thread-info`         | Shows current thread info (virtual vs platform) |
| `GET /api/demo/scoped-value-demo`   | Demonstrates ScopedValue propagation            |
| `GET /api/demo/parallel-processing` | Shows parallel task execution timing            |
| `GET /api/demo/health`              | Health check with Java version info             |

### Customer Endpoints

| Endpoint                            | Description                                            |
|-------------------------------------|--------------------------------------------------------|
| `GET /api/customers`                | List all customers                                     |
| `GET /api/customers/{id}`           | Get customer by ID                                     |
| `GET /api/customers/{id}/dashboard` | **Structured Concurrency demo** - Aggregated dashboard |
| `POST /api/customers`               | Create customer                                        |
| `PUT /api/customers/{id}`           | Update customer                                        |
| `DELETE /api/customers/{id}`        | Delete customer                                        |

### Product Endpoints

| Endpoint                                            | Description                                            |
|-----------------------------------------------------|--------------------------------------------------------|
| `GET /api/products`                                 | List all products                                      |
| `GET /api/products/{id}`                            | Get product by ID                                      |
| `GET /api/products/{id}/inventory/check?quantity=N` | **`Joiner.anySuccessfulResultOrThrow()` demo** - First available warehouse |
| `GET /api/products/{id}/inventory/aggregated`       | **`StructuredTaskScope.open()` demo** - All warehouses      |
| `GET /api/products/search?keyword=X`                | Search products                                        |
| `GET /api/products/low-stock?threshold=N`           | Find low stock items                                   |

### Order Endpoints

| Endpoint                               | Description                                 |
|----------------------------------------|---------------------------------------------|
| `GET /api/orders`                      | List all orders                             |
| `GET /api/orders/{id}`                 | Get order details                           |
| `POST /api/orders`                     | **Parallel validation demo** - Create order |
| `PUT /api/orders/{id}/status?status=X` | Update order status                         |
| `POST /api/orders/{id}/cancel`         | Cancel order                                |

## Example Requests

### 1. Check Thread Info (Virtual Threads)

```bash
curl http://localhost:8080/api/demo/thread-info
```

Response:

```json
{
  "threadName": "tomcat-handler-0",
  "threadId": 42,
  "isVirtual": true,
  "requestId": "abc-123",
  "correlationId": "xyz-789"
}
```

### 2. Scoped Value Propagation Demo

```bash
curl http://localhost:8080/api/demo/scoped-value-demo
```

Shows that child tasks inherit the same `requestId` from the parent via ScopedValue.

### 3. Customer Dashboard (Structured Concurrency)

```bash
curl http://localhost:8080/api/customers/1/dashboard
```

Fetches customer details, recent orders, stats, and recommendations in parallel.

### 4. Inventory Check (Joiner.anySuccessfulResultOrThrow)

```bash
curl "http://localhost:8080/api/products/1/inventory/check?quantity=5"
```

Races multiple warehouses, returns first with sufficient stock.

### 5. Create Order (Parallel Validation)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      {"productId": 1, "quantity": 2},
      {"productId": 2, "quantity": 1}
    ],
    "notes": "Please gift wrap"
  }'
```

Validates all items in parallel using virtual threads.

## Key Code Examples

### Structured Concurrency with StructuredTaskScope.open() (JDK 25)

```java
// Default scope: all tasks must succeed (replaces ShutdownOnFailure)
try (var scope = StructuredTaskScope.open()) {
    var task1 = scope.fork(() -> fetchOrders(customerId));
    var task2 = scope.fork(() -> calculateStats(customerId));
    var task3 = scope.fork(() -> getRecommendations(customerId));

    scope.join();  // Waits for all tasks, throws FailedException on failure

    return new Dashboard(task1.get(), task2.get(), task3.get());
}
```

### ScopedValue Declaration and Usage (JDK 25)

```java
// Declaration
public static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();

// Binding (in filter) - JDK 25 uses where().run()
ScopedValue.where(REQUEST_CONTEXT, context).run(() -> {
    filterChain.doFilter(request, response);
});

// Access (anywhere in the call stack)
RequestContext ctx = ScopedValues.REQUEST_CONTEXT.get();
```

### First Wins with Joiner.anySuccessfulResultOrThrow() (JDK 25)

```java
// Replaces ShutdownOnSuccess - first successful result wins
try (var scope = StructuredTaskScope.open(
        StructuredTaskScope.Joiner.<Result>anySuccessfulResultOrThrow())) {

    scope.fork(() -> checkWarehouse("EAST", productId));
    scope.fork(() -> checkWarehouse("WEST", productId));
    scope.fork(() -> checkWarehouse("CENTRAL", productId));

    return scope.join();  // Returns first successful result directly
}
```

## Configuration

### application.properties

```properties
# Enable Virtual Threads
spring.threads.virtual.enabled=true
# MySQL Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/orderdb
spring.datasource.username=orderuser
spring.datasource.password=orderpass
# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
```

## Performance Benefits

| Scenario                   | Platform Threads        | Virtual Threads                 |
|----------------------------|-------------------------|---------------------------------|
| 10,000 concurrent requests | ~10,000 threads (heavy) | ~10,000 virtual threads (light) |
| Memory per thread          | ~1MB stack              | ~few KB                         |
| Context switching          | OS-level (slow)         | JVM-level (fast)                |
| Blocking I/O               | Thread blocked          | Carrier thread released         |

## Continuous Integration

The [`Java CI with Maven`](.github/workflows/maven.yml) workflow runs on every push / pull request to `main`
(and on manual dispatch):

- Sets up **Temurin JDK 25** with Maven dependency caching
- Runs `./mvnw -B -ntp verify` (compile with `--enable-preview`, tests on H2, package)
- Uploads Surefire reports as an artifact when the build fails
- On pushes to `main`, submits the Maven dependency graph to GitHub for better Dependabot alerts
  (non-blocking; enable **Settings → Code security → Dependency graph** for it to take effect)
- Uses least-privilege `permissions`, cancels superseded runs, and has a job timeout

## Dependency Updates (Dependabot)

[`dependabot.yml`](.github/dependabot.yml) keeps everything up to date with grouped, labeled PRs:

| Ecosystem        | Schedule (Asia/Jakarta) | Grouping                                                      | Commit prefix                  |
|------------------|-------------------------|---------------------------------------------------------------|--------------------------------|
| `maven`          | Daily, 05:00            | `spring`, `test` and `maven-minor-patch` (majors stay single) | `build(deps)` / `build(deps-dev)` |
| `github-actions` | Weekly (Mon), 05:00     | All actions in one PR                                         | `ci(deps)`                     |
| `docker-compose` | Weekly (Mon), 05:00     | MySQL image in `compose.yaml`                                 | `build(deps)`                  |

## Troubleshooting

- **`This project requires JDK 25 or newer`** - point `JAVA_HOME` to a JDK 25 installation.
- **`... is a preview API and is disabled by default`** - run with `--enable-preview`
  (handled automatically by `./mvnw spring-boot:run`; for `java -jar` use
  `java --enable-preview -jar target/*.jar`).
- **Bean named `requestContextFilter` already defined** - the custom filter is registered as
  `scopedValueRequestContextFilter` to avoid clashing with Spring MVC's built-in `requestContextFilter`.

## License

MIT License
