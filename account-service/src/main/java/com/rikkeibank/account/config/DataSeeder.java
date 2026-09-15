package com.rikkeibank.account.config;

import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountStatus;
import com.rikkeibank.account.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AccountRepository accountRepository;

    @Override
    public void run(String... args) {
        if (accountRepository.count() == 0) {
            accountRepository.save(Account.builder().accountNumber("RKB2026CUST0001").customerId(1L)
                    .accountTypeId(1L).balance(new BigDecimal("10000000.00")).status(AccountStatus.ACTIVE).build());
            accountRepository.save(Account.builder().accountNumber("RKB2026CUST0002").customerId(2L)
                    .accountTypeId(1L).balance(new BigDecimal("5000000.00")).status(AccountStatus.ACTIVE).build());
        }
    }
}
