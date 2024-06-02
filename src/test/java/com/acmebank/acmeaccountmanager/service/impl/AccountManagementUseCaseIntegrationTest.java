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
    void shouldStoreDeductTransactionLogWhenTransferMoneyToAnotherAccount() {
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
            transactionLogRepository.findAllByOperatingAccountUserIdOrderByCreateDateTimeUtcDesc(accountOwnerUserId);
        TransactionLogEntity deductLog =
            actualTransactionLogs.stream().filter(log -> log.getOperation().equals("DEDUCT")).findFirst().orElseThrow();
        assertAll(
            () -> assertThat(actualTransactionLogs).hasSizeGreaterThanOrEqualTo(1),
            () -> assertThat(deductLog.getOperatingAccountUserId()).isEqualTo(accountOwnerUserId),
            () -> assertThat(deductLog.getOperatingAccountId()).isEqualTo(accountId1),
            () -> assertThat(deductLog.getOperation()).isEqualTo("DEDUCT"),
            () -> assertThat(deductLog.getCounterpartAccountId()).isEqualTo(accountId2),
            () -> assertThat(deductLog.getReferenceCode()).startsWith("TRANSFER_"),
            () -> assertThat(deductLog.getReferenceCode()).hasSizeGreaterThan(20),
            () -> assertThat(deductLog.getCurrencyCode()).isEqualTo("HKD"),
            () -> assertThat(deductLog.getMoneyAmount()).isEqualTo(BigDecimal.valueOf(50.05)),
            () -> assertThat(deductLog.getCreateDateTimeUtc()).isNotNull()
        );
    }

    @Test
    void shouldStoreAddTransactionLogWhenReceiveMoneyFromTransfer() {
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
            transactionLogRepository.findAllByOperatingAccountUserIdOrderByCreateDateTimeUtcDesc(accountOwnerUserId);
        TransactionLogEntity addLog =
            actualTransactionLogs.stream().filter(log -> log.getOperation().equals("ADD")).findFirst().orElseThrow();
        assertAll(
            () -> assertThat(actualTransactionLogs).hasSizeGreaterThanOrEqualTo(1),
            () -> assertThat(addLog.getOperatingAccountUserId()).isEqualTo(accountOwnerUserId),
            () -> assertThat(addLog.getOperatingAccountId()).isEqualTo(accountId2),
            () -> assertThat(addLog.getOperation()).isEqualTo("ADD"),
            () -> assertThat(addLog.getCounterpartAccountId()).isEqualTo(accountId1),
            () -> assertThat(addLog.getReferenceCode()).startsWith("TRANSFER_"),
            () -> assertThat(addLog.getCurrencyCode()).isEqualTo("HKD"),
            () -> assertThat(addLog.getMoneyAmount()).isEqualTo(BigDecimal.valueOf(50.05)),
            () -> assertThat(addLog.getCreateDateTimeUtc()).isNotNull()
        );
    }

    @Test
    void shouldStoreAddNDeductTransactionLogInPairOrderByCreateDateTimeDescWhenTransferMoneyToAnotherAccount() {
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
            transactionLogRepository.findAllByOperatingAccountUserIdOrderByCreateDateTimeUtcDesc(accountOwnerUserId);
        assertAll(
            () -> assertThat(actualTransactionLogs).hasSize(2),
            () -> assertThat(actualTransactionLogs.get(0).getOperatingAccountUserId())
                .isEqualTo(actualTransactionLogs.get(1).getOperatingAccountUserId()),
            () -> assertThat(actualTransactionLogs.get(0).getOperatingAccountId())
                .isEqualTo(actualTransactionLogs.get(1).getCounterpartAccountId()),
            () -> assertThat(actualTransactionLogs.get(0).getOperation()).isEqualTo("ADD"),
            () -> assertThat(actualTransactionLogs.get(1).getOperation()).isEqualTo("DEDUCT"),
            () -> assertThat(actualTransactionLogs.get(0).getCounterpartAccountId())
                .isEqualTo(actualTransactionLogs.get(1).getOperatingAccountId()),
            () -> assertThat(actualTransactionLogs.get(0).getReferenceCode())
                .startsWith(actualTransactionLogs.get(1).getReferenceCode()),
            () -> assertThat(actualTransactionLogs.get(0).getCurrencyCode())
                .isEqualTo(actualTransactionLogs.get(1).getCurrencyCode()),
            () -> assertThat(actualTransactionLogs.get(0).getMoneyAmount())
                .isEqualTo(actualTransactionLogs.get(1).getMoneyAmount()),
            () -> assertThat(actualTransactionLogs.get(0).getCreateDateTimeUtc())
                .isAfter(actualTransactionLogs.get(1).getCreateDateTimeUtc())
        );
    }

    @Test
    void shouldStoreTransactionLogForRecipientUserWhenTransferMoneyToAnotherAccountOfDifferentUser() {
        // given
        final UUID accountUserId1 = UUID.randomUUID();
        final UUID accountUserId2 = UUID.randomUUID();
        final String accountId1 = "12345678" + UUID.randomUUID();
        final String accountId2 = "88888888" + UUID.randomUUID();
        setupAccount(accountUserId1, accountId1, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(accountUserId2, accountId2, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));

        // when
        accountManagement.transferMoneyToAccount(AccountManagement.TransferMoneyToAccountRequest.builder()
            .userId(accountUserId1)
            .operatingAccountId(accountId1)
            .operatingAccountVersion(1)
            .recipientAccountId(accountId2)
            .currencyCode("HKD")
            .toBeTransferAmount(BigDecimal.valueOf(50.05))
            .build());

        // then
        List<TransactionLogEntity> actualSenderLogs =
            transactionLogRepository.findAllByOperatingAccountUserIdOrderByCreateDateTimeUtcDesc(accountUserId1);
        List<TransactionLogEntity> actualReceiverLogs =
            transactionLogRepository.findAllByOperatingAccountUserIdOrderByCreateDateTimeUtcDesc(accountUserId2);
        assertAll(
            () -> assertThat(actualSenderLogs).hasSize(1),
            () -> assertThat(actualReceiverLogs).hasSize(1),
            () -> assertThat(actualSenderLogs.get(0).getOperatingAccountUserId())
                .isNotEqualTo(actualReceiverLogs.get(0).getOperatingAccountUserId()),
            () -> assertThat(actualSenderLogs.get(0).getOperatingAccountUserId()).isEqualTo(accountUserId1),
            () -> assertThat(actualReceiverLogs.get(0).getOperatingAccountUserId()).isEqualTo(accountUserId2),
            () -> assertThat(actualSenderLogs.get(0).getOperatingAccountId())
                .isEqualTo(actualReceiverLogs.get(0).getCounterpartAccountId()),
            () -> assertThat(actualSenderLogs.get(0).getOperation()).isEqualTo("DEDUCT"),
            () -> assertThat(actualReceiverLogs.get(0).getOperation()).isEqualTo("ADD"),
            () -> assertThat(actualSenderLogs.get(0).getCounterpartAccountId())
                .isEqualTo(actualReceiverLogs.get(0).getOperatingAccountId()),
            () -> assertThat(actualSenderLogs.get(0).getReferenceCode())
                .startsWith(actualReceiverLogs.get(0).getReferenceCode()),
            () -> assertThat(actualSenderLogs.get(0).getCurrencyCode())
                .isEqualTo(actualReceiverLogs.get(0).getCurrencyCode()),
            () -> assertThat(actualSenderLogs.get(0).getMoneyAmount())
                .isEqualTo(actualReceiverLogs.get(0).getMoneyAmount()),
            () -> assertThat(actualReceiverLogs.get(0).getCreateDateTimeUtc())
                .isAfter(actualSenderLogs.get(0).getCreateDateTimeUtc())
        );
    }

}