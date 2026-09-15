package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.AccountTypeDtos.AccountTypeRequest;
import com.rikkeibank.customer.entity.AccountType;
import com.rikkeibank.customer.exception.EntityNotFoundException;
import com.rikkeibank.customer.repository.AccountTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountTypeService {

    private final AccountTypeRepository accountTypeRepository;

    public List<AccountType> findAll() {
        return accountTypeRepository.findAll();
    }

    public AccountType findById(Long id) {
        return accountTypeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AccountType not found: " + id));
    }

    public AccountType create(AccountTypeRequest request) {
        AccountType type = AccountType.builder()
                .code(request.code())
                .name(request.name())
                .interestRate(request.interestRate())
                .description(request.description())
                .build();
        return accountTypeRepository.save(type);
    }

    public AccountType update(Long id, AccountTypeRequest request) {
        AccountType type = findById(id);
        type.setCode(request.code());
        type.setName(request.name());
        type.setInterestRate(request.interestRate());
        type.setDescription(request.description());
        return accountTypeRepository.save(type);
    }

    public void delete(Long id) {
        accountTypeRepository.delete(findById(id));
    }
}
