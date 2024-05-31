package com.acmebank.acmeaccountmanager.service.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.UUID;

public interface AccountManagement {

    MoneyAccount getAccount(@Valid GetMoneyAccountRequest request);

    @Builder
    record GetMoneyAccountRequest(
        @NotNull UUID id,
        // TODO: authorization checking should be in place
        @NotNull UUID userId
    ) {
    }

}
