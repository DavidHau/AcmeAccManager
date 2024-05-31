package com.acmebank.acmeaccountmanager.service.api;

import lombok.Builder;
import org.javamoney.moneta.Money;

import java.util.UUID;

@Builder
public record MoneyAccount(
    UUID id,
    Integer version,
    UUID primaryOwnerId,
    String currencyCode,
    Money balance
) {
}
