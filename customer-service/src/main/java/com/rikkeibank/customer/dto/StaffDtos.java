package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.NotBlank;

public class StaffDtos {
    public record StaffRequest(@NotBlank String fullName, @NotBlank String username,
                                @NotBlank String branch, String role) {}
}
