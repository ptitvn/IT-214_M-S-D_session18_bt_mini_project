package com.rikkeibank.transaction.controller;

import com.rikkeibank.transaction.dto.TransactionDtos.TransferRequest;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.exception.EntityNotFoundException;
import com.rikkeibank.transaction.repository.TransactionRepository;
import com.rikkeibank.transaction.saga.TransferSagaOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransferSagaOrchestrator sagaOrchestrator;
    private final TransactionRepository transactionRepository;

    /** CUSTOMER initiates a transfer to any destination account. TELLER can also perform it on behalf of a customer. */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('CUSTOMER','TELLER','ADMIN')")
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request,
                                                 @RequestHeader("X-User-Role") String role,
                                                 @RequestHeader(value = "X-User-Id", required = false) String userId) {
        Long tellerId = "TELLER".equals(role) && userId != null ? Long.valueOf(userId) : null;
        Long customerId = "CUSTOMER".equals(role) && userId != null ? Long.valueOf(userId) : null;
        Transaction result = sagaOrchestrator.executeTransfer(
                request.fromAccountId(), request.toAccountId(), request.amount(), customerId, tellerId);
        return ResponseEntity.status(201).body(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public Transaction findById(@PathVariable Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found: " + id));
    }

    /** "Giao dịch viên có thể xem danh sách giao dịch đã thực hiện trong ngày" */
    @GetMapping("/daily")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER')")
    public List<Transaction> dailyTransactions(@RequestHeader("X-User-Role") String role,
                                                @RequestHeader(value = "X-User-Id", required = false) String userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        if ("TELLER".equals(role) && userId != null) {
            // A teller only sees the transfers they personally handled (never another teller's), per SRS.
            return transactionRepository.findByTellerIdAndCreatedAtBetween(Long.valueOf(userId), startOfDay, endOfDay);
        }
        return transactionRepository.findByCreatedAtBetween(startOfDay, endOfDay);
    }

    /** CUSTOMER's own transaction history. */
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public ResponseEntity<List<Transaction>> byCustomer(@PathVariable Long customerId,
                                                         @RequestHeader("X-User-Role") String role,
                                                         @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if ("CUSTOMER".equals(role) && (userId == null || !userId.equals(String.valueOf(customerId)))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(transactionRepository.findByInitiatedByCustomerId(customerId));
    }
}
