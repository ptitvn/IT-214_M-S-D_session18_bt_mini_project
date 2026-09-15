package com.rikkeibank.identity.config;

import com.rikkeibank.identity.entity.Role;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seeds demo accounts so the whole flow can be tested end-to-end out of the box. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seed("admin", "admin123", Role.ADMIN, 1L);
        seed("teller1", "teller123", Role.TELLER, 1L);
        seed("customer1", "customer123", Role.CUSTOMER, 1L);
        seed("customer2", "customer123", Role.CUSTOMER, 2L);
    }

    private void seed(String username, String rawPassword, Role role, Long referenceId) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .referenceId(referenceId)
                    .enabled(true)
                    .build());
        }
    }
}
