package com.acmebank.acmeaccountmanager.service.impl;

import com.acmebank.acmeaccountmanager.service.api.AccountManagement;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import jakarta.validation.ConstraintViolationException;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
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

    @Autowired
    TransactionLogRepository transactionLogRepository;

    void setupAccount(UUID userId, String accountId, Money balance) {
        MoneyAccountEntity accountEntity = MoneyAccountEntity.builder()
            .id(accountId)
            .version(1)
            .primaryOwnerId(userId)
            .currencyCode(balance.getCurrency().getCurrencyCode())
            .balanceAmount(balance.getNumberStripped())
            .build();
        moneyAccountRepository.save(accountEntity);
    }

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

    @Test
    void shouldStoreTransactionLogWhenTransferMoneyToAnotherAccount() {
        // given
        final UUID accountOwnerUserId = UUID.randomUUID();
        final String accountId1 = "12345678" + UUID.randomUUID();
        final String accountId2 = "88888888" + UUID.randomUUID();
        setupAccount(accountOwnerUserId, accountId1, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(accountOwnerUserId, accountId2, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));

        // when
        accountManagement.transferMoneyToAccount(AccountManagement.TransferMoneyToAccountRequest.builder()
            .userId(accountOwnerUserId)
            .operatingAccountId(accountId1)
            .operatingAccountVersion(1)
            .recipientAccountId(accountId2)
            .currencyCode("HKD")
            .toBeTransferAmount(BigDecimal.valueOf(50.05))
            .build());

        // then
        List<TransactionLogEntity> actualTransactionLogs =
            transactionLogRepository.findAllByOperatorUserId(accountOwnerUserId);
        assertAll(
            () -> assertThat(actualTransactionLogs).hasSize(1),
            () -> assertThat(actualTransactionLogs.get(0).getOperatorUserId()).isEqualTo(accountOwnerUserId),
            () -> assertThat(actualTransactionLogs.get(0).getOperatingAccountId()).isEqualTo(accountId1),
            () -> assertThat(actualTransactionLogs.get(0).getOperation()).isEqualTo("DEDUCT"),
            () -> assertThat(actualTransactionLogs.get(0).getCounterpartAccountId()).isEqualTo(accountId2),
            () -> assertThat(actualTransactionLogs.get(0).getReferenceCode()).startsWith("TRANSFER_"),
            () -> assertThat(actualTransactionLogs.get(0).getCurrencyCode()).isEqualTo("HKD"),
            () -> assertThat(actualTransactionLogs.get(0).getMoneyAmount()).isEqualTo(BigDecimal.valueOf(50.05)),
            () -> assertThat(actualTransactionLogs.get(0).getCreateDateTimeUtc()).isNotNull()
        );
    }

}