package com.rikkeibank.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerDtos {
    public record CustomerRequest(@NotBlank String fullName, @NotBlank String idNumber,
                                   @NotBlank @Email String email, String phone, String address) {}
}
