package com.rikkeibank.notification.controller;

import com.rikkeibank.notification.entity.Notification;
import com.rikkeibank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN','TELLER','CUSTOMER')")
    public ResponseEntity<List<Notification>> byCustomer(@PathVariable Long customerId,
                                                          @RequestHeader("X-User-Role") String role,
                                                          @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if ("CUSTOMER".equals(role) && (userId == null || !userId.equals(String.valueOf(customerId)))) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customerId));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Notification> findAll() {
        return notificationRepository.findAll();
    }
}
