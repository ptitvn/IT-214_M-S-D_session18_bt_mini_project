package com.rikkeibank.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class TransactionDtos {
    public record TransferRequest(@NotNull Long fromAccountId, @NotNull Long toAccountId,
                                   @NotNull @DecimalMin("0.01") BigDecimal amount) {}
}
