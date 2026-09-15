package com.rikkeibank.transaction.client;

import java.math.BigDecimal;

/** Mirrors account-service's Account response shape (only the fields transaction-service needs). */
public record AccountDto(Long id, String accountNumber, Long customerId, Long accountTypeId,
                          BigDecimal balance, String status) {}
