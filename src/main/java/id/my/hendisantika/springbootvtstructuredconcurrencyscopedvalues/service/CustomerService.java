package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.service;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.ScopedValues;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.dto.CustomerDashboard;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Customer;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Order;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Product;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.CustomerRepository;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.OrderRepository;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;

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
 * Service demonstrating Structured Concurrency (JEP 480) for parallel data fetching.
 * <p>
 * Structured Concurrency ensures that:
 * 1. All subtasks complete before the parent task completes
 * 2. If any subtask fails, all other subtasks are cancelled
 * 3. ScopedValues are automatically inherited by child threads
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * Fetches a comprehensive customer dashboard using Structured Concurrency.
     * Multiple data sources are queried in parallel using virtual threads.
     * <p>
     * This demonstrates:
     * - StructuredTaskScope.ShutdownOnFailure for fail-fast behavior
     * - Automatic ScopedValue propagation to subtasks
     * - Virtual thread creation for I/O-bound operations
     */
    @Transactional(readOnly = true)
    public CustomerDashboard getCustomerDashboard(Long customerId) {
        long startTime = System.currentTimeMillis();
        String requestId = ScopedValues.currentRequestContext().requestId();

        log.info("Building customer dashboard for customerId={} [requestId={}]", customerId, requestId);

        // Fetch customer first (required for other operations)
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        // Use Structured Concurrency to fetch related data in parallel
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // Each fork creates a new virtual thread that inherits ScopedValues
            var recentOrdersTask = scope.fork(() -> {
                log.debug("Fetching recent orders on thread: {} [requestId={}]",
                        Thread.currentThread(), ScopedValues.currentRequestContext().requestId());
                return fetchRecentOrders(customerId);
            });

            var statsTask = scope.fork(() -> {
                log.debug("Calculating customer stats on thread: {} [requestId={}]",
                        Thread.currentThread(), ScopedValues.currentRequestContext().requestId());
                return calculateCustomerStats(customerId, customer.getCreatedAt());
            });

            var recommendationsTask = scope.fork(() -> {
                log.debug("Fetching recommendations on thread: {} [requestId={}]",
                        Thread.currentThread(), ScopedValues.currentRequestContext().requestId());
                return getProductRecommendations(customerId);
            });

            // Wait for all tasks to complete or fail
            scope.join();
            scope.throwIfFailed();

            // All tasks completed successfully - build the dashboard
            long fetchTimeMs = System.currentTimeMillis() - startTime;

            CustomerDashboard.CustomerDetails customerDetails = new CustomerDashboard.CustomerDetails(
                    customer.getId(),
                    customer.getFullName(),
                    customer.getEmail(),
                    customer.getPhoneNumber(),
                    customer.getShippingAddress()
            );

            return new CustomerDashboard(
                    customerDetails,
                    recentOrdersTask.get(),
                    statsTask.get(),
                    recommendationsTask.get(),
                    requestId,
                    fetchTimeMs
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Dashboard fetch interrupted", e);
        }
    }

    private List<CustomerDashboard.RecentOrder> fetchRecentOrders(Long customerId) {
        // Simulate some I/O latency
        simulateLatency(50);

        List<Order> orders = orderRepository.findRecentOrdersByCustomerId(customerId);
        return orders.stream()
                .limit(5)
                .map(order -> new CustomerDashboard.RecentOrder(
                        order.getId(),
                        order.getOrderNumber(),
                        order.getStatus().name(),
                        order.getTotalAmount(),
                        order.getCreatedAt().toString(),
                        order.getItems().size()
                ))
                .toList();
    }

    private CustomerDashboard.CustomerStats calculateCustomerStats(Long customerId, LocalDateTime memberSince) {
        // Simulate some I/O latency
        simulateLatency(75);

        List<Order> allOrders = orderRepository.findByCustomerId(customerId);

        long totalOrders = allOrders.size();
        BigDecimal totalSpent = allOrders.stream()
                .map(Order::getTotalAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageOrderValue = totalOrders > 0
                ? totalSpent.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new CustomerDashboard.CustomerStats(
                totalOrders,
                totalSpent,
                averageOrderValue,
                memberSince != null ? memberSince.toString() : "Unknown"
        );
    }

    private List<CustomerDashboard.ProductRecommendation> getProductRecommendations(Long customerId) {
        // Simulate some I/O latency (e.g., calling ML service)
        simulateLatency(100);

        // Simple recommendation: return active products
        List<Product> products = productRepository.findByIsActiveTrue();
        return products.stream()
                .limit(3)
                .map(product -> new CustomerDashboard.ProductRecommendation(
                        product.getId(),
                        product.getName(),
                        product.getCategory(),
                        product.getPrice(),
                        "Popular in your area"
                ))
                .toList();
    }

    private void simulateLatency(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Transactional(readOnly = true)
    public Optional<Customer> findById(Long id) {
        return customerRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional
    public Customer save(Customer customer) {
        customer.setCreatedAt(LocalDateTime.now());
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        customer.setFirstName(customerDetails.getFirstName());
        customer.setLastName(customerDetails.getLastName());
        customer.setEmail(customerDetails.getEmail());
        customer.setPhoneNumber(customerDetails.getPhoneNumber());
        customer.setShippingAddress(customerDetails.getShippingAddress());
        customer.setUpdatedAt(LocalDateTime.now());

        return customerRepository.save(customer);
    }

    @Transactional
    public void delete(Long id) {
        customerRepository.deleteById(id);
    }
}
