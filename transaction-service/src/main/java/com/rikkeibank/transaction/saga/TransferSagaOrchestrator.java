package com.rikkeibank.transaction.saga;

import com.rikkeibank.transaction.client.AccountDto;
import com.rikkeibank.transaction.client.AccountServiceClient;
import com.rikkeibank.transaction.client.MoneyMovementRequest;
import com.rikkeibank.transaction.entity.Transaction;
import com.rikkeibank.transaction.entity.TransactionStatus;
import com.rikkeibank.transaction.exception.AccountServiceUnavailableException;
import com.rikkeibank.transaction.kafka.TransactionEventProducer;
import com.rikkeibank.transaction.repository.TransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SAGA PATTERN — Orchestrator style.
 *
 * transaction-service acts as the orchestrator that drives a multi-step distributed transaction
 * across account-service instances (no cross-service ACID transaction is used):
 *
 *   Step 1: DEBIT   source account   (via account-service, protected by Resilience4j Circuit Breaker)
 *   Step 2: CREDIT  destination account (via account-service, protected by Circuit Breaker)
 *   Compensating step: if Step 2 fails after Step 1 succeeded, COMPENSATE by refunding the source
 *   account (compensate-debit), guaranteeing eventual consistency instead of relying on 2PC/XA.
 *
 * The Circuit Breaker (CLOSED -> OPEN -> HALF_OPEN) prevents a struggling account-service from
 * cascading failures back into transaction-service: once the failure rate crosses the configured
 * threshold, the breaker OPENs and short-circuits to the fallback method immediately, giving
 * account-service room to recover, then trial-probes with HALF_OPEN calls before fully closing again.
 */
@Component
@RequiredArgsConstructor
public class TransferSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaOrchestrator.class);

    private final AccountServiceClient accountServiceClient;
    private final TransactionRepository transactionRepository;
    private final TransactionEventProducer eventProducer;

    @Transactional
    public Transaction executeTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                        Long initiatedByCustomerId, Long tellerId) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        String reference = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        Transaction transaction = Transaction.builder()
                .reference(reference)
                .fromAccountId(fromAccountId)
                .toAccountId(toAccountId)
                .amount(amount)
                .status(TransactionStatus.PENDING)
                .initiatedByCustomerId(initiatedByCustomerId)
                .tellerId(tellerId)
                .build();
        transaction = transactionRepository.save(transaction);

        // ---- Saga Step 1: debit source account ----
        AccountDto debited;
        try {
            debited = callDebit(fromAccountId, amount, reference);
            log.info("[SAGA][{}] Step1 DEBIT ok, account {} new balance {}", reference, fromAccountId, debited.balance());
        } catch (Exception e) {
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setFailureReason("Debit step failed: " + e.getMessage());
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            eventProducer.publishFailed(transaction.getId(), fromAccountId, toAccountId,
                    initiatedByCustomerId, amount.toPlainString(), reference, e.getMessage());
            return transaction;
        }

        // ---- Saga Step 2: credit destination account ----
        try {
            AccountDto credited = callCredit(toAccountId, amount, reference);
            log.info("[SAGA][{}] Step2 CREDIT ok, account {} new balance {}", reference, toAccountId, credited.balance());

            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            eventProducer.publishCompleted(transaction.getId(), fromAccountId, toAccountId,
                    initiatedByCustomerId, amount.toPlainString(), reference);
            return transaction;

        } catch (Exception e) {
            // ---- Compensating transaction: refund the source account (Saga rollback) ----
            log.warn("[SAGA][{}] Step2 CREDIT failed ({}), compensating step1 debit on account {}",
                    reference, e.getMessage(), fromAccountId);
            try {
                accountServiceClient.compensateDebit(fromAccountId,
                        new MoneyMovementRequest(amount, reference + "-COMPENSATE"));
                transaction.setStatus(TransactionStatus.COMPENSATED);
                transaction.setFailureReason("Credit step failed: " + e.getMessage() + " (compensated)");
            } catch (Exception compensateEx) {
                // Worst case: manual reconciliation needed - logged clearly for ops/support.
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setFailureReason("Credit step failed: " + e.getMessage()
                        + " AND compensation failed: " + compensateEx.getMessage() + " - MANUAL RECONCILIATION REQUIRED");
                log.error("[SAGA][{}] COMPENSATION FAILED - manual reconciliation required: {}",
                        reference, compensateEx.getMessage());
            }
            transaction.setCompletedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            eventProducer.publishCompensated(transaction.getId(), fromAccountId, toAccountId,
                    initiatedByCustomerId, amount.toPlainString(), reference, transaction.getFailureReason());
            return transaction;
        }
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "debitFallback")
    public AccountDto callDebit(Long accountId, BigDecimal amount, String reference) {
        return accountServiceClient.debit(accountId, new MoneyMovementRequest(amount, reference));
    }

    @CircuitBreaker(name = "accountService", fallbackMethod = "creditFallback")
    public AccountDto callCredit(Long accountId, BigDecimal amount, String reference) {
        return accountServiceClient.credit(accountId, new MoneyMovementRequest(amount, reference));
    }

    // ---- Resilience4j fallback methods: same signature + trailing Throwable ----

    @SuppressWarnings("unused")
    private AccountDto debitFallback(Long accountId, BigDecimal amount, String reference, Throwable t) {
        log.error("Circuit breaker OPEN or call failed for DEBIT on account {}: {}", accountId, t.getMessage());
        throw new AccountServiceUnavailableException(
                "account-service is currently unavailable (circuit breaker engaged) - debit aborted", t);
    }

    @SuppressWarnings("unused")
    private AccountDto creditFallback(Long accountId, BigDecimal amount, String reference, Throwable t) {
        log.error("Circuit breaker OPEN or call failed for CREDIT on account {}: {}", accountId, t.getMessage());
        throw new AccountServiceUnavailableException(
                "account-service is currently unavailable (circuit breaker engaged) - credit aborted", t);
    }
}
