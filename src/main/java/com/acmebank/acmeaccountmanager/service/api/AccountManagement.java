package com.acmebank.acmeaccountmanager.service.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface AccountManagement {

    MoneyAccount getAccount(@Valid GetMoneyAccountRequest request);

    List<MoneyAccount> getAllAccounts(@NotNull UUID userId);

    void transferMoneyToAccount(@Valid TransferMoneyToAccountRequest request);

    @Builder
    record GetMoneyAccountRequest(
        @NotNull String id,
        @NotNull UUID userId
    ) {
    }

    @Builder
    record TransferMoneyToAccountRequest(
        @NotNull String operatingAccountId,
        @NotNull Integer operatingAccountVersion,
        @NotNull String recipientAccountId,
        @NotNull String currencyCode,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal toBeTransferAmount,
        @NotNull UUID userId
    ) {
    }
}
