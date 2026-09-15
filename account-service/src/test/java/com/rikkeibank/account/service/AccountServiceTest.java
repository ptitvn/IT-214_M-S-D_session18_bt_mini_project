package com.rikkeibank.account.service;

import com.rikkeibank.account.entity.Account;
import com.rikkeibank.account.entity.AccountStatus;
import com.rikkeibank.account.exception.InsufficientBalanceException;
import com.rikkeibank.account.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.builder()
                .id(1L)
                .accountNumber("RKB2026TEST0001")
                .customerId(1L)
                .accountTypeId(1L)
                .balance(new BigDecimal("1000000.00"))
                .status(AccountStatus.ACTIVE)
                .build();
    }

    @Test
    void debit_shouldReduceBalance_whenSufficientFunds() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.debit(1L, new BigDecimal("300000.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("700000.00");
    }

    @Test
    void debit_shouldThrow_whenBalanceInsufficient() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.debit(1L, new BigDecimal("5000000.00")))
                .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test
    void credit_shouldIncreaseBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        Account result = accountService.credit(1L, new BigDecimal("250000.00"));

        assertThat(result.getBalance()).isEqualByComparingTo("1250000.00");
    }
}
