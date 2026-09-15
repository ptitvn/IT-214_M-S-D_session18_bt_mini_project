package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.StaffDtos.StaffRequest;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    public List<Staff> findAll() {
        return staffService.findAll();
    }

    @GetMapping("/{id}")
    public Staff findById(@PathVariable Long id) {
        return staffService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Staff> create(@Valid @RequestBody StaffRequest request) {
        return ResponseEntity.status(201).body(staffService.create(request));
    }

    @PutMapping("/{id}")
    public Staff update(@PathVariable Long id, @Valid @RequestBody StaffRequest request) {
        return staffService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        staffService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
