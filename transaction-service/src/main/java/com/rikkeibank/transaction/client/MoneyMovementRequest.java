package com.rikkeibank.transaction.client;

import java.math.BigDecimal;

public record MoneyMovementRequest(BigDecimal amount, String reference) {}
