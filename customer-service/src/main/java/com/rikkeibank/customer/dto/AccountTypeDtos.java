package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.NotBlank;

public class AccountTypeDtos {
    public record AccountTypeRequest(@NotBlank String code, @NotBlank String name,
                                      Double interestRate, String description) {}
}
