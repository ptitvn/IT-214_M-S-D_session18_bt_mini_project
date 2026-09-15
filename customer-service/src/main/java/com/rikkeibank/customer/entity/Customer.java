package com.rikkeibank.customer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String idNumber; // CMND/CCCD

    @Column(nullable = false, unique = true)
    private String email;

    private String phone;

    private String address;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
