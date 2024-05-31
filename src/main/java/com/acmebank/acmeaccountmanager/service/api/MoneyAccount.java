package com.acmebank.acmeaccountmanager.service.api;

import lombok.Builder;
import org.javamoney.moneta.Money;

import java.util.UUID;

@Builder
public record MoneyAccount(
    String id,
    Integer version,
    UUID primaryOwnerId,
    String currencyCode,
    Money balance
) {
}
