package com.rikkeibank.customer.config;

import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.entity.Customer;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.AccountTypeRepository;
import com.rikkeibank.customer.repository.CustomerRepository;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Seeds demo catalog data matching the identity-service demo users (customer1 -> id 1, customer2 -> id 2). */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final AccountTypeRepository accountTypeRepository;

    @Override
    public void run(String... args) {
        if (customerRepository.count() == 0) {
            customerRepository.save(Customer.builder().fullName("Nguyen Van A").idNumber("001099012345")
                    .email("customer1@rikkeibank.vn").phone("0900000001").address("Ha Noi").build());
            customerRepository.save(Customer.builder().fullName("Tran Thi B").idNumber("001099054321")
                    .email("customer2@rikkeibank.vn").phone("0900000002").address("Ho Chi Minh").build());
        }
        if (staffRepository.count() == 0) {
            staffRepository.save(Staff.builder().fullName("Le Van Teller").username("teller1")
                    .branch("Chi nhanh Hoan Kiem").role("TELLER").build());
        }
        if (accountTypeRepository.count() == 0) {
            accountTypeRepository.save(AccountType.builder().code("CHECKING").name("Tai khoan thanh toan")
                    .interestRate(0.0).description("Non-interest checking account").build());
            accountTypeRepository.save(AccountType.builder().code("SAVING").name("Tai khoan tiet kiem")
                    .interestRate(4.5).description("Savings account with interest").build());
        }
    }
}
