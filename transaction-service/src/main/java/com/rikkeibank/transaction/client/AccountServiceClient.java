package com.rikkeibank.transaction.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * Synchronous service-to-service communication (per SRS): OpenFeign + Spring Cloud LoadBalancer,
 * calling account-service purely by its Eureka-registered name "account-service" — no hardcoded host/port.
 */
@FeignClient(name = "account-service")
public interface AccountServiceClient {

    @GetMapping("/api/accounts/{id}")
    AccountDto getAccount(@PathVariable("id") Long id);

    @PostMapping("/api/accounts/{id}/debit")
    AccountDto debit(@PathVariable("id") Long id, @RequestBody MoneyMovementRequest request);

    @PostMapping("/api/accounts/{id}/credit")
    AccountDto credit(@PathVariable("id") Long id, @RequestBody MoneyMovementRequest request);

    @PostMapping("/api/accounts/{id}/compensate-debit")
    AccountDto compensateDebit(@PathVariable("id") Long id, @RequestBody MoneyMovementRequest request);
}
