package com.rikkeibank.account.service;

import com.rikkeibank.account.dto.AccountDtos.CreateAccountRequest;
import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountStatus;
import com.rikkeibank.account.exception.EntityNotFoundException;
import com.rikkeibank.account.exception.InsufficientBalanceException;
import com.rikkeibank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Distributed Caching strategy: Cache-Aside via Spring Cache + Redis.
 * - @Cacheable on plain reads (getById): checks cache first, falls back to DB, then populates cache.
 * - @CachePut on debit/credit: always executes, then refreshes the cache with the new balance.
 * - @CacheEvict on delete/close: removes the now-invalid entry.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final String CACHE_NAME = "accounts";

    private final AccountRepository accountRepository;

    public List<Account> findAll() {
        return accountRepository.findAll();
    }

    public List<Account> findByCustomerId(Long customerId) {
        return accountRepository.findByCustomerId(customerId);
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#id")
    public Account findById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
    }

    @Transactional
    public Account create(CreateAccountRequest request) {
        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .customerId(request.customerId())
                .accountTypeId(request.accountTypeId())
                .balance(request.initialBalance())
                .status(AccountStatus.ACTIVE)
                .build();
        return accountRepository.save(account);
    }

    @CacheEvict(cacheNames = CACHE_NAME, key = "#id")
    @Transactional
    public void delete(Long id) {
        Account account = findByIdUncached(id);
        accountRepository.delete(account);
    }

    /** Called by transaction-service Saga step 1: reserve/debit funds from the source account. */
    @CachePut(cacheNames = CACHE_NAME, key = "#id")
    @Transactional
    public Account debit(Long id, BigDecimal amount) {
        Account account = findByIdUncached(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account " + id + " is not ACTIVE");
        }
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account " + id);
        }
        account.setBalance(account.getBalance().subtract(amount));
        return accountRepository.save(account);
    }

    /** Called by transaction-service Saga step 2: credit the destination account. */
    @CachePut(cacheNames = CACHE_NAME, key = "#id")
    @Transactional
    public Account credit(Long id, BigDecimal amount) {
        Account account = findByIdUncached(id);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account " + id + " is not ACTIVE");
        }
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    /** Compensating transaction (Saga rollback): refund the source account if a later step fails. */
    @CachePut(cacheNames = CACHE_NAME, key = "#id")
    @Transactional
    public Account compensateDebit(Long id, BigDecimal amount) {
        Account account = findByIdUncached(id);
        account.setBalance(account.getBalance().add(amount));
        return accountRepository.save(account);
    }

    private Account findByIdUncached(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + id));
    }

    private String generateAccountNumber() {
        return "RKB" + LocalDateTime.now().getYear() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
