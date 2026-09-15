package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.StaffDtos.StaffRequest;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.exception.EntityNotFoundException;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository staffRepository;

    public List<Staff> findAll() {
        return staffRepository.findAll();
    }

    public Staff findById(Long id) {
        return staffRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Staff not found: " + id));
    }

    public Staff create(StaffRequest request) {
        Staff staff = Staff.builder()
                .fullName(request.fullName())
                .username(request.username())
                .branch(request.branch())
                .role(request.role() == null ? "TELLER" : request.role())
                .build();
        return staffRepository.save(staff);
    }

    public Staff update(Long id, StaffRequest request) {
        Staff staff = findById(id);
        staff.setFullName(request.fullName());
        staff.setUsername(request.username());
        staff.setBranch(request.branch());
        if (request.role() != null) staff.setRole(request.role());
        return staffRepository.save(staff);
    }

    public void delete(Long id) {
        staffRepository.delete(findById(id));
    }
}
