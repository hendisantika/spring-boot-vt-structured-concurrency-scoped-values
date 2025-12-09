package id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.controller;

import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.dto.CustomerDashboard;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.entity.Customer;
import id.my.hendisantika.springbootvtstructuredconcurrencyscopedvalues.service.CustomerService;
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
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Get customer dashboard with aggregated data.
     * This endpoint demonstrates Structured Concurrency - multiple data sources
     * are queried in parallel using virtual threads.
     */
    @GetMapping("/{id}/dashboard")
    public ResponseEntity<CustomerDashboard> getCustomerDashboard(@PathVariable Long id) {
        log.info("Request received for customer dashboard: {}", id);
        CustomerDashboard dashboard = customerService.getCustomerDashboard(id);
        return ResponseEntity.ok(dashboard);
    }

    @GetMapping
    public ResponseEntity<List<Customer>> getAllCustomers() {
        return ResponseEntity.ok(customerService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Customer> getCustomerById(@PathVariable Long id) {
        return customerService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Customer> createCustomer(@Valid @RequestBody Customer customer) {
        Customer savedCustomer = customerService.save(customer);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Customer> updateCustomer(
            @PathVariable Long id,
            @Valid @RequestBody Customer customer) {
        try {
            Customer updatedCustomer = customerService.update(id, customer);
            return ResponseEntity.ok(updatedCustomer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
