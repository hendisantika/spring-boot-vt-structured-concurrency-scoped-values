# Spring Boot 4 - Virtual Threads, Structured Concurrency & Scoped Values Demo

A comprehensive demonstration of JDK 25's modern concurrency features with Spring Boot 4 and MySQL 9.

## Features Demonstrated

### 1. Virtual Threads (JEP 444)

- Enabled via `spring.threads.virtual.enabled=true`
- Tomcat handles all HTTP requests on virtual threads
- Lightweight threads ideal for I/O-bound operations

### 2. Structured Concurrency (JEP 480)

- **ShutdownOnFailure**: All subtasks must succeed, fails fast on first error
- **ShutdownOnSuccess**: First successful result wins, cancels remaining tasks
- Automatic cleanup of child threads when parent completes

### 3. Scoped Values (JEP 481)

- Request context automatically propagated to child threads
- Immutable, inherited values across virtual thread hierarchies
- Better alternative to ThreadLocal for virtual threads

## Technology Stack

- **Java**: 25
- **Spring Boot**: 4.0.0
- **Database**: MySQL 9.0.0
- **Build Tool**: Maven

## Project Structure

```
src/main/java/id/my/hendisantika/springbootvtstructuredconcurrencyscopedvalues/
├── config/
│   ├── DataInitializer.java        # Sample data loader
│   ├── GlobalExceptionHandler.java # Exception handling
│   └── RequestContextFilter.java   # ScopedValue binding filter
├── context/
│   ├── RequestContext.java         # Request context record
│   └── ScopedValues.java           # ScopedValue definitions
├── controller/
│   ├── CustomerController.java
│   ├── DemoController.java         # Feature demonstration endpoints
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
    ├── CustomerService.java        # Structured Concurrency example
    ├── InventoryCheckService.java  # ShutdownOnSuccess example
    ├── OrderService.java           # Parallel validation example
    └── ProductService.java
```

## Getting Started

### Prerequisites

- JDK 25 (Early Access)
- Docker & Docker Compose
- Maven 3.9+

### Running the Application

1. **Start MySQL using Docker Compose:**
   ```bash
   docker-compose up -d
   ```

2. **Run the Spring Boot application:**
   ```bash
   ./mvnw spring-boot:run
   ```

3. **Access the API:**
    - Base URL: `http://localhost:8080`
    - Demo endpoints: `http://localhost:8080/api/demo/*`

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
| `GET /api/products/{id}/inventory/check?quantity=N` | **ShutdownOnSuccess demo** - First available warehouse |
| `GET /api/products/{id}/inventory/aggregated`       | **ShutdownOnFailure demo** - All warehouses            |
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

### 4. Inventory Check (ShutdownOnSuccess)

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

### Structured Concurrency with ShutdownOnFailure

```java
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    var task1 = scope.fork(() -> fetchOrders(customerId));
    var task2 = scope.fork(() -> calculateStats(customerId));
    var task3 = scope.fork(() -> getRecommendations(customerId));

    scope.join();           // Wait for all tasks
    scope.throwIfFailed();  // Propagate any exceptions

    return new Dashboard(task1.get(), task2.get(), task3.get());
}
```

### ScopedValue Declaration and Usage

```java
// Declaration
public static final ScopedValue<RequestContext> REQUEST_CONTEXT = ScopedValue.newInstance();

// Binding (in filter)
ScopedValue.runWhere(REQUEST_CONTEXT, context, () -> filterChain.doFilter(...));

// Access (anywhere in the call stack)
RequestContext ctx = ScopedValues.REQUEST_CONTEXT.get();
```

### ShutdownOnSuccess (First Wins)

```java
try(var scope = new StructuredTaskScope.ShutdownOnSuccess<Result>()){
        scope.

fork(() ->

checkWarehouse("EAST",productId));
        scope.

fork(() ->

checkWarehouse("WEST",productId));
        scope.

fork(() ->

checkWarehouse("CENTRAL",productId));

        scope.

join();
    return scope.

result();  // First successful result
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

## License

MIT License
