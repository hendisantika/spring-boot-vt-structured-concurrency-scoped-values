package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.config;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Customer;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Order;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.OrderItem;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Product;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.CustomerRepository;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.OrderRepository;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-vt-structured-concurrency-scoped-values
 * User: hendisantika
 * Link: s.id/hendisantika
 * Email: hendisantika@yahoo.co.id
 * Telegram : @hendisantika34
 * Date: 09/12/25
 * Time: 08.30
 * To change this template use File | Settings | File Templates.
 */

/**
 * Initializes sample data for development and testing.
 * Only runs when the "dev" profile is active.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            log.info("Database already contains data, skipping initialization");
            return;
        }

        log.info("Initializing sample data...");

        // Create customers
        List<Customer> customers = createCustomers();
        customerRepository.saveAll(customers);
        log.info("Created {} customers", customers.size());

        // Create products
        List<Product> products = createProducts();
        productRepository.saveAll(products);
        log.info("Created {} products", products.size());

        // Create orders
        List<Order> orders = createOrders(customers, products);
        orderRepository.saveAll(orders);
        log.info("Created {} orders", orders.size());

        log.info("Sample data initialization completed!");
    }

    private List<Customer> createCustomers() {
        return List.of(
                Customer.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .email("john.doe@example.com")
                        .phoneNumber("+1-555-0101")
                        .shippingAddress("123 Main St, New York, NY 10001")
                        .createdAt(LocalDateTime.now().minusMonths(6))
                        .build(),
                Customer.builder()
                        .firstName("Jane")
                        .lastName("Smith")
                        .email("jane.smith@example.com")
                        .phoneNumber("+1-555-0102")
                        .shippingAddress("456 Oak Ave, Los Angeles, CA 90001")
                        .createdAt(LocalDateTime.now().minusMonths(3))
                        .build(),
                Customer.builder()
                        .firstName("Bob")
                        .lastName("Johnson")
                        .email("bob.johnson@example.com")
                        .phoneNumber("+1-555-0103")
                        .shippingAddress("789 Pine Rd, Chicago, IL 60601")
                        .createdAt(LocalDateTime.now().minusMonths(1))
                        .build(),
                Customer.builder()
                        .firstName("Alice")
                        .lastName("Williams")
                        .email("alice.williams@example.com")
                        .phoneNumber("+1-555-0104")
                        .shippingAddress("321 Elm St, Houston, TX 77001")
                        .createdAt(LocalDateTime.now().minusWeeks(2))
                        .build(),
                Customer.builder()
                        .firstName("Charlie")
                        .lastName("Brown")
                        .email("charlie.brown@example.com")
                        .phoneNumber("+1-555-0105")
                        .shippingAddress("654 Maple Dr, Phoenix, AZ 85001")
                        .createdAt(LocalDateTime.now().minusDays(5))
                        .build()
        );
    }

    private List<Product> createProducts() {
        return List.of(
                Product.builder()
                        .name("Wireless Bluetooth Headphones")
                        .description("High-quality wireless headphones with noise cancellation")
                        .sku("ELEC-WBH-001")
                        .price(new BigDecimal("149.99"))
                        .stockQuantity(100)
                        .category("Electronics")
                        .isActive(true)
                        .createdAt(LocalDateTime.now().minusMonths(2))
                        .build(),
                Product.builder()
                        .name("Smart Watch Series 5")
                        .description("Feature-rich smartwatch with health monitoring")
                        .sku("ELEC-SW5-002")
                        .price(new BigDecimal("299.99"))
                        .stockQuantity(50)
                        .category("Electronics")
                        .isActive(true)
                        .createdAt(LocalDateTime.now().minusMonths(1))
                        .build(),
                Product.builder()
                        .name("Organic Coffee Beans 1kg")
                        .description("Premium Arabica coffee beans from Colombia")
                        .sku("FOOD-OCB-001")
                        .price(new BigDecimal("24.99"))
                        .stockQuantity(200)
                        .category("Food & Beverage")
                        .isActive(true)
                        .createdAt(LocalDateTime.now().minusWeeks(3))
                        .build(),
                Product.builder()
                        .name("Yoga Mat Premium")
                        .description("Extra thick, non-slip yoga mat with carrying strap")
                        .sku("SPRT-YMP-001")
                        .price(new BigDecimal("39.99"))
                        .stockQuantity(75)
                        .category("Sports & Fitness")
                        .isActive(true)
                        .createdAt(LocalDateTime.now().minusWeeks(2))
                        .build(),
                Product.builder()
                        .name("Stainless Steel Water Bottle")
                        .description("Insulated 32oz water bottle, keeps drinks cold for 24 hours")
                        .sku("HOME-SSB-001")
                        .price(new BigDecimal("29.99"))
                        .stockQuantity(150)
                        .category("Home & Kitchen")
                        .isActive(true)
                        .createdAt(LocalDateTime.now().minusDays(10))
                        .build(),
                Product.builder()
                        .name("Programming in Java 21+")
                        .description("Comprehensive guide to modern Java features")
                        .sku("BOOK-PJ21-001")
                        .price(new BigDecimal("49.99"))
                        .stockQuantity(30)
                        .category("Books")
                        .isActive(true)
                        .createdAt(LocalDateTime.now().minusDays(5))
                        .build(),
                Product.builder()
                        .name("USB-C Hub 7-in-1")
                        .description("Multi-port USB-C hub with HDMI, USB-A, and SD card reader")
                        .sku("ELEC-UCH-003")
                        .price(new BigDecimal("59.99"))
                        .stockQuantity(80)
                        .category("Electronics")
                        .isActive(true)
                        .createdAt(LocalDateTime.now().minusDays(3))
                        .build(),
                Product.builder()
                        .name("Discontinued Item")
                        .description("This product is no longer available")
                        .sku("DISC-XXX-001")
                        .price(new BigDecimal("9.99"))
                        .stockQuantity(5)
                        .category("Clearance")
                        .isActive(false)
                        .createdAt(LocalDateTime.now().minusMonths(6))
                        .build()
        );
    }

    private List<Order> createOrders(List<Customer> customers, List<Product> products) {
        List<Order> orders = new ArrayList<>();

        // Order 1: John Doe - Multiple items
        Order order1 = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customers.get(0))
                .status(Order.OrderStatus.DELIVERED)
                .shippingAddress(customers.get(0).getShippingAddress())
                .notes("Please leave at door")
                .createdAt(LocalDateTime.now().minusDays(30))
                .deliveredAt(LocalDateTime.now().minusDays(25))
                .items(new ArrayList<>())
                .build();

        addOrderItem(order1, products.get(0), 1); // Headphones
        addOrderItem(order1, products.get(4), 2); // Water bottles
        order1.calculateTotalAmount();
        orders.add(order1);

        // Order 2: Jane Smith - Single item
        Order order2 = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customers.get(1))
                .status(Order.OrderStatus.SHIPPED)
                .shippingAddress(customers.get(1).getShippingAddress())
                .createdAt(LocalDateTime.now().minusDays(5))
                .shippedAt(LocalDateTime.now().minusDays(3))
                .items(new ArrayList<>())
                .build();

        addOrderItem(order2, products.get(1), 1); // Smart Watch
        order2.calculateTotalAmount();
        orders.add(order2);

        // Order 3: Bob Johnson - Processing
        Order order3 = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customers.get(2))
                .status(Order.OrderStatus.PROCESSING)
                .shippingAddress(customers.get(2).getShippingAddress())
                .createdAt(LocalDateTime.now().minusDays(2))
                .items(new ArrayList<>())
                .build();

        addOrderItem(order3, products.get(2), 3); // Coffee beans
        addOrderItem(order3, products.get(5), 1); // Java book
        order3.calculateTotalAmount();
        orders.add(order3);

        // Order 4: Alice Williams - Pending
        Order order4 = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customers.get(3))
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(customers.get(3).getShippingAddress())
                .createdAt(LocalDateTime.now().minusHours(2))
                .items(new ArrayList<>())
                .build();

        addOrderItem(order4, products.get(3), 1); // Yoga mat
        addOrderItem(order4, products.get(6), 1); // USB-C Hub
        order4.calculateTotalAmount();
        orders.add(order4);

        // Order 5: John Doe - Another order (showing customer with multiple orders)
        Order order5 = Order.builder()
                .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .customer(customers.get(0))
                .status(Order.OrderStatus.CONFIRMED)
                .shippingAddress(customers.get(0).getShippingAddress())
                .createdAt(LocalDateTime.now().minusDays(1))
                .items(new ArrayList<>())
                .build();

        addOrderItem(order5, products.get(5), 2); // Java book x2
        order5.calculateTotalAmount();
        orders.add(order5);

        return orders;
    }

    private void addOrderItem(Order order, Product product, int quantity) {
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(quantity)
                .unitPrice(product.getPrice())
                .build();
        item.calculateSubtotal();
        order.addItem(item);
    }
}
