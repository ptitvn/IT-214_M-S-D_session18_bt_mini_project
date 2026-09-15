package com.rikkeibank.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class AccountDtos {

    public record CreateAccountRequest(@NotNull Long customerId, @NotNull Long accountTypeId,
                                        @NotNull @DecimalMin("0.0") BigDecimal initialBalance) {}

    public record MoneyMovementRequest(@NotNull @DecimalMin(value = "0.01") BigDecimal amount,
                                        @NotBlank String reference) {}

    public record MessageResponse(String message) {}
}
