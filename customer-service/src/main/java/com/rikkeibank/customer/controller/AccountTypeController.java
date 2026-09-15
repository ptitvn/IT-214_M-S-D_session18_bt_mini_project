package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.AccountTypeDtos.AccountTypeRequest;
import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.service.AccountTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/account-types")
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public List<AccountType> findAll() {
        return accountTypeService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public AccountType findById(@PathVariable Long id) {
        return accountTypeService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccountType> create(@Valid @RequestBody AccountTypeRequest request) {
        return ResponseEntity.status(201).body(accountTypeService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountType update(@PathVariable Long id, @Valid @RequestBody AccountTypeRequest request) {
        return accountTypeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
