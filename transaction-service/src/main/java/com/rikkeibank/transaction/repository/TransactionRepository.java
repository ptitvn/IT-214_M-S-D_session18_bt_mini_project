package com.rikkeibank.transaction.repository;

import com.rikkeibank.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Transaction> findByInitiatedByCustomerId(Long customerId);
    List<Transaction> findByTellerIdAndCreatedAtBetween(Long tellerId, LocalDateTime start, LocalDateTime end);
}
