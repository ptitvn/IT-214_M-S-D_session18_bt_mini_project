package com.rikkeibank.identity.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Links this login account to the business record (Customer.id or Staff.id in their own services) */
    private Long referenceId;

    @Builder.Default
    private boolean enabled = true;

    private String refreshToken;

    private LocalDateTime refreshTokenExpiry;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
