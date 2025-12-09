package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.service;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.RequestContext;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.ScopedValues;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.dto.OrderRequest;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.dto.OrderResponse;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Customer;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Order;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.OrderItem;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Product;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.CustomerRepository;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.OrderRepository;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
 * Order service demonstrating Structured Concurrency with ShutdownOnSuccess policy.
 * <p>
 * This demonstrates:
 * - StructuredTaskScope.ShutdownOnSuccess for "first wins" scenarios
 * - Parallel validation using virtual threads
 * - ScopedValue access within subtasks
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    /**
     * Creates a new order with parallel validation of items.
     * Uses Structured Concurrency to validate all items concurrently.
     */
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        RequestContext context = ScopedValues.currentRequestContext();
        log.info("Creating order for customer {} [requestId={}]",
                request.customerId(), context.requestId());

        // Validate customer exists
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + request.customerId()));

        // Validate all items in parallel using Structured Concurrency
        List<ValidatedItem> validatedItems = validateItemsInParallel(request.items());

        // Create the order
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .shippingAddress(request.shippingAddress() != null
                        ? request.shippingAddress()
                        : customer.getShippingAddress())
                .notes(request.notes())
                .status(Order.OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        // Add validated items to order
        for (ValidatedItem validatedItem : validatedItems) {
            OrderItem orderItem = OrderItem.builder()
                    .product(validatedItem.product())
                    .quantity(validatedItem.quantity())
                    .unitPrice(validatedItem.product().getPrice())
                    .build();
            orderItem.calculateSubtotal();
            order.addItem(orderItem);

            // Decrease stock
            productRepository.decreaseStock(validatedItem.product().getId(), validatedItem.quantity());
        }

        // Calculate total
        order.calculateTotalAmount();

        // Save order
        Order savedOrder = orderRepository.save(order);

        log.info("Order created: {} [requestId={}]", savedOrder.getOrderNumber(), context.requestId());

        return OrderResponse.from(savedOrder, context.requestId());
    }

    /**
     * Validates all order items in parallel using Structured Concurrency.
     * All validations must succeed, otherwise the entire operation fails.
     */
    private List<ValidatedItem> validateItemsInParallel(List<OrderRequest.OrderItemRequest> items) {
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            // Fork a validation task for each item
            List<StructuredTaskScope.Subtask<ValidatedItem>> tasks = items.stream()
                    .map(item -> scope.fork(() -> validateItem(item)))
                    .toList();

            // Wait for all validations to complete
            scope.join();
            scope.throwIfFailed();

            // Collect results
            return tasks.stream()
                    .map(StructuredTaskScope.Subtask::get)
                    .toList();

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Order validation interrupted", e);
        }
    }

    private ValidatedItem validateItem(OrderRequest.OrderItemRequest item) {
        String requestId = ScopedValues.currentRequestContext().requestId();
        log.debug("Validating item productId={} quantity={} on thread {} [requestId={}]",
                item.productId(), item.quantity(), Thread.currentThread(), requestId);

        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + item.productId()));

        if (!product.getIsActive()) {
            throw new IllegalArgumentException("Product is not active: " + product.getName());
        }

        if (!product.hasEnoughStock(item.quantity())) {
            throw new IllegalArgumentException(
                    "Insufficient stock for product: " + product.getName() +
                            " (available: " + product.getStockQuantity() +
                            ", requested: " + item.quantity() + ")");
        }

        return new ValidatedItem(product, item.quantity());
    }

    /**
     * Finds an order by ID with full details, demonstrating parallel data enrichment.
     */
    @Transactional(readOnly = true)
    public Optional<OrderResponse> findByIdWithDetails(Long id) {
        return orderRepository.findByIdWithItems(id)
                .map(order -> {
                    // Initialize lazy-loaded associations
                    order.getCustomer().getFullName();
                    order.getItems().forEach(item -> item.getProduct().getName());
                    return OrderResponse.from(order, ScopedValues.currentRequestContext().requestId());
                });
    }

    @Transactional(readOnly = true)
    public Optional<OrderResponse> findByOrderNumber(String orderNumber) {
        return orderRepository.findByOrderNumberWithDetails(orderNumber)
                .map(order -> OrderResponse.from(order, ScopedValues.currentRequestContext().requestId()));
    }

    @Transactional(readOnly = true)
    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Order> findByStatus(Order.OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateStatus(Long orderId, Order.OrderStatus newStatus) {
        String requestId = ScopedValues.currentRequestContext().requestId();
        log.info("Updating order {} status to {} [requestId={}]", orderId, newStatus, requestId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());

        if (newStatus == Order.OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());
        } else if (newStatus == Order.OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        String requestId = ScopedValues.currentRequestContext().requestId();
        log.info("Cancelling order {} [requestId={}]", orderId, requestId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getStatus() == Order.OrderStatus.SHIPPED ||
                order.getStatus() == Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel order that has been shipped or delivered");
        }

        // Restore stock for each item
        for (OrderItem item : order.getItems()) {
            productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private record ValidatedItem(Product product, int quantity) {
    }
}
