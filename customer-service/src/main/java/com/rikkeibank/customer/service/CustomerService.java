package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.CustomerDtos.CustomerRequest;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.exception.EntityNotFoundException;
import com.rikkeibank.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Customer not found: " + id));
    }

    public Customer create(CustomerRequest request) {
        Customer customer = Customer.builder()
                .fullName(request.fullName())
                .idNumber(request.idNumber())
                .email(request.email())
                .phone(request.phone())
                .address(request.address())
                .build();
        return customerRepository.save(customer);
    }

    public Customer update(Long id, CustomerRequest request) {
        Customer customer = findById(id);
        customer.setFullName(request.fullName());
        customer.setIdNumber(request.idNumber());
        customer.setEmail(request.email());
        customer.setPhone(request.phone());
        customer.setAddress(request.address());
        return customerRepository.save(customer);
    }

    public void delete(Long id) {
        Customer customer = findById(id);
        customerRepository.delete(customer);
    }
}
