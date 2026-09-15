package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.CustomerDtos.CustomerRequest;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ADMIN: full CRUD.
 * TELLER: read-only (needs customer info while assisting transactions).
 * CUSTOMER: read-only, restricted to their own record (enforced by X-User-Id check).
 */
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public List<Customer> findAll() {
        return customerService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public ResponseEntity<Customer> findById(@PathVariable Long id,
                                              @RequestHeader("X-User-Role") String role,
                                              @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if ("CUSTOMER".equals(role) && (userId == null || !userId.equals(String.valueOf(id)))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(customerService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Customer> create(@Valid @RequestBody CustomerRequest request) {
        return ResponseEntity.status(201).body(customerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Customer update(@PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return customerService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
