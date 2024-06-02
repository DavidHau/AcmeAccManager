package com.acmebank.acmeaccountmanager.service.api;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record TransactionLog(
    UUID id,
    String operatingAccountId,
    String operation,
    UUID operatorUserId,
    String referenceCode,
    String counterpartAccountId,
    String currencyCode,
    BigDecimal moneyAmount,
    Instant createDateTimeUtc
) {
}
