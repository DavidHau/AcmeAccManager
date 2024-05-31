package com.acmebank.acmeaccountmanager.service.impl;

import com.acmebank.acmeaccountmanager.service.api.AccountManagement;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AccountManagementUseCaseIntegrationTest {

    @Autowired
    AccountManagement accountManagement;

    @Autowired
    MoneyAccountRepository moneyAccountRepository;

    @Test
    void shouldSaveAndGetAccountWithBalanceIn2DecimalPlaces() {
        // given
        final double amountWith4dp = 0500.1234;
        final UUID userId = UUID.randomUUID();
        final String moneyAccountId = UUID.randomUUID().toString();
        MoneyAccountEntity dummyAccount = MoneyAccountEntity.builder()
            .id(moneyAccountId)
            .version(1)
            .primaryOwnerId(userId)
            .currencyCode("HKD")
            .balanceAmount(BigDecimal.valueOf(amountWith4dp))
            .build();
        moneyAccountRepository.save(dummyAccount);

        // when
        MoneyAccount actualAccount = accountManagement.getAccount(AccountManagement.GetMoneyAccountRequest.builder()
            .id(moneyAccountId)
            .userId(userId)
            .build());

        // then
        assertAll(
            () -> assertThat(actualAccount).isNotNull(),
            () -> assertThat(actualAccount.id()).isEqualTo(moneyAccountId),
            () -> assertThat(actualAccount.primaryOwnerId()).isEqualTo(userId),
            () -> assertThat(actualAccount.version()).isEqualTo(1),
            () -> assertThat(actualAccount.currencyCode()).isEqualTo("HKD"),
            () -> assertThat(actualAccount.balance().getCurrency().getCurrencyCode()).isEqualTo("HKD"),
            () -> assertThat(actualAccount.balance().getNumberStripped()).isEqualTo(BigDecimal.valueOf(500.12))
        );
    }

    @Test
    void userIdMustBeProvidedWhenGetAccount() {
        final String moneyAccountId = UUID.randomUUID().toString();
        assertThrows(ConstraintViolationException.class,
            () -> accountManagement.getAccount(AccountManagement.GetMoneyAccountRequest.builder()
                .id(moneyAccountId)
                .build()));
    }

}