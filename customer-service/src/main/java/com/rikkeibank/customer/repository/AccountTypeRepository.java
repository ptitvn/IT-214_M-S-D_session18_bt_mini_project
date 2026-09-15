package com.rikkeibank.customer.repository;

import com.rikkeibank.customer.entity.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountTypeRepository extends JpaRepository<AccountType, Long> {
}
