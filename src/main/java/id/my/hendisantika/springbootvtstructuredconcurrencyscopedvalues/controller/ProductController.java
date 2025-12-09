package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.controller;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Product;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.service.InventoryCheckService;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final InventoryCheckService inventoryCheckService;

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        return ResponseEntity.ok(productService.findAll());
    }

    @GetMapping("/active")
    public ResponseEntity<List<Product>> getActiveProducts() {
        return ResponseEntity.ok(productService.findActiveProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/sku/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        return productService.findBySku(sku)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(productService.findByCategory(category));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        return ResponseEntity.ok(productService.findLowStockProducts(threshold));
    }

    /**
     * Check inventory across multiple warehouses using ShutdownOnSuccess.
     * Returns the first warehouse that has sufficient stock.
     */
    @GetMapping("/{id}/inventory/check")
    public ResponseEntity<InventoryCheckService.InventoryResult> checkInventory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int quantity) {
        log.info("Checking inventory for product {} with quantity {}", id, quantity);
        try {
            InventoryCheckService.InventoryResult result =
                    inventoryCheckService.findAvailableInventory(id, quantity);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * Get aggregated inventory from all warehouses using ShutdownOnFailure.
     * Waits for all warehouse responses.
     */
    @GetMapping("/{id}/inventory/aggregated")
    public ResponseEntity<InventoryCheckService.AggregatedInventory> getAggregatedInventory(
            @PathVariable Long id) {
        log.info("Getting aggregated inventory for product {}", id);
        InventoryCheckService.AggregatedInventory inventory =
                inventoryCheckService.getAggregatedInventory(id);
        return ResponseEntity.ok(inventory);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) {
        Product savedProduct = productService.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody Product product) {
        try {
            Product updatedProduct = productService.update(id, product);
            return ResponseEntity.ok(updatedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}/stock/decrease")
    public ResponseEntity<Void> decreaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        boolean success = productService.decreaseStock(id, quantity);
        return success
                ? ResponseEntity.ok().build()
                : ResponseEntity.badRequest().build();
    }

    @PutMapping("/{id}/stock/increase")
    public ResponseEntity<Void> increaseStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        productService.increaseStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
