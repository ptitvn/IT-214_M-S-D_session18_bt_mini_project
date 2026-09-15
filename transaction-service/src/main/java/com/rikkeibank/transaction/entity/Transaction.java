package com.rikkeibank.transaction.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A Transaction record captures the outcome of a Saga run (debit + credit across two accounts).
 * status tracks the Saga's current state instead of a separate SagaState table, to keep the
 * schema simple: PENDING -> SUCCESS, or PENDING -> FAILED -> COMPENSATED after rollback.
 */
@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private Long fromAccountId;

    @Column(nullable = false)
    private Long toAccountId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    private String failureReason;

    /** Which TELLER (if any) handled/witnessed this transfer; null for self-service customer transfers. */
    private Long tellerId;

    private Long initiatedByCustomerId;
//bcao
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime completedAt;
}
