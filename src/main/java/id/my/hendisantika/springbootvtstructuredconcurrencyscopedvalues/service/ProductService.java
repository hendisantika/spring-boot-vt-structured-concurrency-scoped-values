package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.service;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.context.ScopedValues;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Product;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Optional<Product> findById(Long id) {
        log.debug("Finding product by id={} [requestId={}]",
                id, ScopedValues.currentRequestContext().requestId());
        return productRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findBySku(String sku) {
        return productRepository.findBySku(sku);
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Product> findActiveProducts() {
        return productRepository.findByIsActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Product> findByCategory(String category) {
        return productRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProducts(String keyword) {
        return productRepository.searchProducts(keyword);
    }

    @Transactional(readOnly = true)
    public List<Product> findLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold);
    }

    @Transactional
    public Product save(Product product) {
        log.info("Creating new product: {} [requestId={}]",
                product.getName(), ScopedValues.currentRequestContext().requestId());
        product.setCreatedAt(LocalDateTime.now());
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, Product productDetails) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        log.info("Updating product: {} [requestId={}]",
                product.getName(), ScopedValues.currentRequestContext().requestId());

        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setSku(productDetails.getSku());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setCategory(productDetails.getCategory());
        product.setImageUrl(productDetails.getImageUrl());
        product.setIsActive(productDetails.getIsActive());
        product.setUpdatedAt(LocalDateTime.now());

        return productRepository.save(product);
    }

    @Transactional
    public boolean decreaseStock(Long productId, int quantity) {
        log.info("Decreasing stock for product {} by {} [requestId={}]",
                productId, quantity, ScopedValues.currentRequestContext().requestId());
        int updated = productRepository.decreaseStock(productId, quantity);
        return updated > 0;
    }

    @Transactional
    public void increaseStock(Long productId, int quantity) {
        log.info("Increasing stock for product {} by {} [requestId={}]",
                productId, quantity, ScopedValues.currentRequestContext().requestId());
        productRepository.increaseStock(productId, quantity);
    }

    @Transactional
    public void delete(Long id) {
        log.info("Deleting product: {} [requestId={}]",
                id, ScopedValues.currentRequestContext().requestId());
        productRepository.deleteById(id);
    }
}
