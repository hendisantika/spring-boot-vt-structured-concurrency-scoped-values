package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.service;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.ScopedValues;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Product;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
 * Service demonstrating StructuredTaskScope.ShutdownOnSuccess pattern.
 * <p>
 * ShutdownOnSuccess is useful for "race" scenarios where you want the first
 * successful result and can cancel the remaining tasks.
 * <p>
 * Use cases:
 * - Querying multiple data sources and using the first response
 * - Finding any available inventory from multiple warehouses
 * - Redundant service calls for fault tolerance
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCheckService {

    private final ProductRepository productRepository;

    /**
     * Checks inventory across multiple simulated "warehouses" in parallel.
     * Returns as soon as any warehouse reports sufficient stock.
     * <p>
     * This demonstrates ShutdownOnSuccess - the first warehouse to confirm
     * stock availability wins, and other checks are cancelled.
     */
    @Transactional(readOnly = true)
    public InventoryResult findAvailableInventory(Long productId, int requiredQuantity) {
        String requestId = ScopedValues.currentRequestContext().requestId();
        log.info("Checking inventory for product {} (qty: {}) [requestId={}]",
                productId, requiredQuantity, requestId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        try (var scope = new StructuredTaskScope.ShutdownOnSuccess<InventoryResult>()) {

            // Simulate checking multiple warehouses in parallel
            scope.fork(() -> checkWarehouse("WAREHOUSE-EAST", product, requiredQuantity, 100));
            scope.fork(() -> checkWarehouse("WAREHOUSE-WEST", product, requiredQuantity, 150));
            scope.fork(() -> checkWarehouse("WAREHOUSE-CENTRAL", product, requiredQuantity, 75));

            scope.join();

            // Get the first successful result
            InventoryResult result = scope.result();
            log.info("Found inventory at {} for product {} [requestId={}]",
                    result.warehouseId(), productId, requestId);
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Inventory check interrupted", e);
        } catch (Exception e) {
            log.warn("No warehouse has sufficient inventory for product {} [requestId={}]",
                    productId, requestId);
            throw new IllegalStateException("No warehouse has sufficient inventory", e);
        }
    }

    private InventoryResult checkWarehouse(String warehouseId, Product product,
                                           int requiredQuantity, long simulatedLatencyMs) {
        String requestId = ScopedValues.currentRequestContext().requestId();
        log.debug("Checking {} for product {} [requestId={}]",
                warehouseId, product.getId(), requestId);

        // Simulate network latency to warehouse system
        try {
            Thread.sleep(simulatedLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Warehouse check interrupted", e);
        }

        // Simulate warehouse-specific stock (in reality, this would call an external service)
        int availableStock = simulateWarehouseStock(warehouseId, product);

        if (availableStock >= requiredQuantity) {
            return new InventoryResult(
                    warehouseId,
                    product.getId(),
                    product.getName(),
                    availableStock,
                    requiredQuantity,
                    true,
                    "Stock available"
            );
        }

        // Throw exception to indicate this warehouse doesn't have enough stock
        throw new InsufficientStockException(warehouseId, availableStock, requiredQuantity);
    }

    private int simulateWarehouseStock(String warehouseId, Product product) {
        // Simulate different stock levels at different warehouses
        int baseStock = product.getStockQuantity();
        return switch (warehouseId) {
            case "WAREHOUSE-EAST" -> baseStock + 10;
            case "WAREHOUSE-WEST" -> baseStock + 5;
            case "WAREHOUSE-CENTRAL" -> baseStock + 15;
            default -> baseStock;
        };
    }

    /**
     * Aggregates inventory data from all warehouses using ShutdownOnFailure.
     * This demonstrates when you need ALL results vs just the first one.
     */
    @Transactional(readOnly = true)
    public AggregatedInventory getAggregatedInventory(Long productId) {
        String requestId = ScopedValues.currentRequestContext().requestId();
        log.info("Getting aggregated inventory for product {} [requestId={}]", productId, requestId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            var eastTask = scope.fork(() -> getWarehouseInfo("WAREHOUSE-EAST", product, 50));
            var westTask = scope.fork(() -> getWarehouseInfo("WAREHOUSE-WEST", product, 75));
            var centralTask = scope.fork(() -> getWarehouseInfo("WAREHOUSE-CENTRAL", product, 60));

            scope.join();
            scope.throwIfFailed();

            List<WarehouseInfo> warehouses = List.of(
                    eastTask.get(),
                    westTask.get(),
                    centralTask.get()
            );

            int totalStock = warehouses.stream()
                    .mapToInt(WarehouseInfo::availableQuantity)
                    .sum();

            return new AggregatedInventory(
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    totalStock,
                    warehouses,
                    requestId
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Aggregated inventory check interrupted", e);
        }
    }

    private WarehouseInfo getWarehouseInfo(String warehouseId, Product product, long latencyMs) {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Warehouse info fetch interrupted", e);
        }

        int stock = simulateWarehouseStock(warehouseId, product);
        return new WarehouseInfo(warehouseId, stock, stock > 0);
    }

    public record InventoryResult(
            String warehouseId,
            Long productId,
            String productName,
            int availableQuantity,
            int requestedQuantity,
            boolean available,
            String message
    ) {
    }

    public record AggregatedInventory(
            Long productId,
            String productName,
            String sku,
            int totalAvailableQuantity,
            List<WarehouseInfo> warehouses,
            String requestId
    ) {
    }

    public record WarehouseInfo(
            String warehouseId,
            int availableQuantity,
            boolean hasStock
    ) {
    }

    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String warehouseId, int available, int required) {
            super(String.format("Warehouse %s has insufficient stock (available: %d, required: %d)",
                    warehouseId, available, required));
        }
    }
}
