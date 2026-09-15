package com.rikkeibank.account.controller;

import com.rikkeibank.account.dto.AccountDtos.CreateAccountRequest;
import com.rikkeibank.account.dto.AccountDtos.MoneyMovementRequest;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public List<Account> findAll() {
        return accountService.findAll();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public ResponseEntity<Account> findById(@PathVariable Long id,
                                             @RequestHeader("X-User-Role") String role,
                                             @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Account account = accountService.findById(id);
        if ("CUSTOMER".equals(role) && (userId == null || !userId.equals(String.valueOf(account.getCustomerId())))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(account);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public ResponseEntity<List<Account>> findByCustomer(@PathVariable Long customerId,
                                                         @RequestHeader("X-User-Role") String role,
                                                         @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if ("CUSTOMER".equals(role) && (userId == null || !userId.equals(String.valueOf(customerId)))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(accountService.findByCustomerId(customerId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Account> create(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(201).body(accountService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- Internal endpoints used only by transaction-service's Saga orchestrator ----

    @PostMapping("/{id}/debit")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','TELLER')")
    public Account debit(@PathVariable Long id, @Valid @RequestBody MoneyMovementRequest request) {
        return accountService.debit(id, request.amount());
    }

    @PostMapping("/{id}/credit")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','TELLER')")
    public Account credit(@PathVariable Long id, @Valid @RequestBody MoneyMovementRequest request) {
        return accountService.credit(id, request.amount());
    }

    @PostMapping("/{id}/compensate-debit")
    @PreAuthorize("hasAnyRole('ADMIN','CUSTOMER','TELLER')")
    public Account compensateDebit(@PathVariable Long id, @Valid @RequestBody MoneyMovementRequest request) {
        return accountService.compensateDebit(id, request.amount());
    }
}
