package com.rikkeibank.transaction.exception;

/** Raised when account-service is unreachable or its Circuit Breaker is OPEN. */
public class AccountServiceUnavailableException extends RuntimeException {
    public AccountServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
