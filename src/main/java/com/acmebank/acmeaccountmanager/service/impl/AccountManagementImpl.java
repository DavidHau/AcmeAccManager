package com.acmebank.acmeaccountmanager.service.impl;

import com.acmebank.acmeaccountmanager.service.api.AccountManagement;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Validated
@Transactional
class AccountManagementImpl implements AccountManagement {


    @Override
    public MoneyAccount getAccount(GetMoneyAccountRequest request) {
        final UUID moneyAccountId = request.id();
        final UUID userId = request.userId();

        MoneyAccount dummyAccount = MoneyAccount.builder()
            .id(moneyAccountId)
            .version(1)
            .primaryOwnerId(userId)
            .currencyCode("HKD")
            .balance(Money.of(BigDecimal.valueOf(0500.100), "HKD"))
            .build();
        return dummyAccount;
    }
}
