package com.acmebank.acmeaccountmanager.service.impl;

import com.acmebank.acmeaccountmanager.service.api.AccountManagement;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import com.acmebank.acmeaccountmanager.service.api.TransactionLog;
import com.acmebank.acmeaccountmanager.service.exception.InsufficientBalanceErrorException;
import com.acmebank.acmeaccountmanager.service.impl.mapper.AccountManagementImplMapper;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Validated
@Transactional
class AccountManagementImpl implements AccountManagement {

    private final MoneyAccountRepository moneyAccountRepository;
    private final AccountManagementImplMapper mapper;
    private final AuthorizationValidationService authorizationValidationService;
    private final ReferenceCodeGenerator referenceCodeGenerator;
    private final TransactionLogRepository transactionLogRepository;

    public AccountManagementImpl(
        MoneyAccountRepository moneyAccountRepository,
        AccountManagementImplMapper mapper,
        TransactionLogRepository transactionLogRepository
    ) {
        this.moneyAccountRepository = moneyAccountRepository;
        this.mapper = mapper;
        this.authorizationValidationService = new AuthorizationValidationService();
        this.referenceCodeGenerator = new ReferenceCodeGenerator();
        this.transactionLogRepository = transactionLogRepository;
    }

    @Override
    public MoneyAccount getAccount(GetMoneyAccountRequest request) {
        final String moneyAccountId = request.id();
        final UUID userId = request.userId();

        MoneyAccountEntity moneyAccountEntity = getMoneyAccountEntityOrThrow(moneyAccountId);
        MoneyAccount moneyAccount = mapper.entityToDomainObject(moneyAccountEntity);
        authorizationValidationService.ensureHasReadAccess(moneyAccount, userId);
        return moneyAccount;
    }

    private MoneyAccountEntity getMoneyAccountEntityOrThrow(String moneyAccountId) {
        return moneyAccountRepository.findById(moneyAccountId)
            .orElseThrow(
                () -> new EntityNotFoundException("MoneyAccount[%s] does not exist!".formatted(moneyAccountId)));
    }

    @Override
    public List<MoneyAccount> getAllAccounts(UUID userId) {
        return moneyAccountRepository.findAllByPrimaryOwnerIdOrderById(userId)
            .stream().map(mapper::entityToDomainObject)
            .toList();
    }

    @Override
    public void transferMoneyToAccount(TransferMoneyToAccountRequest request) {
        final UUID operatingUserId = request.userId();
        final MoneyAccountEntity operatingAccount = getMoneyAccountEntityOrThrow(request.operatingAccountId());
        final Integer operatingAccountVersion = request.operatingAccountVersion();
        final MoneyAccountEntity recipientAccount = getMoneyAccountEntityOrThrow(request.recipientAccountId());
        final Money toBeTransferMoney = Money.of(request.toBeTransferAmount(), request.currencyCode());

        final String operationType = "TRANSFER";
        final String transactionCode = "%s_%s".formatted(operationType, referenceCodeGenerator.generate(20));
        deductMoney(operatingAccount, operatingAccountVersion, toBeTransferMoney, operatingUserId,
            transactionCode, recipientAccount.getId());
        addMoney(recipientAccount, toBeTransferMoney, operatingUserId,
            transactionCode, operatingAccount.getId());
    }

    private void deductMoney(MoneyAccountEntity account, int versionNumber, Money amount, UUID userId,
                             String transactionCode, String counterpartAccountId) {
        authorizationValidationService.ensureHasMoneyDeductionAccess(account, userId);
        if (!account.getVersion().equals(versionNumber)) {
            throw new OptimisticLockException(
                "MoneyAccountId: %s, versionNumber: %s".formatted(account.getId(), versionNumber));
        }
        Money newBalance = account.getBalance().subtract(amount);
        if (newBalance.isNegative()) {
            throw new InsufficientBalanceErrorException(account.getId());
        }
        account.setBalanceAmount(newBalance.getNumberStripped());
        moneyAccountRepository.save(account);
        transactionLogRepository.save(TransactionLogEntity.builder()
            .operatingAccountId(account.getId())
            .operation("DEDUCT")
            .operatorUserId(userId)
            .referenceCode(transactionCode)
            .counterpartAccountId(counterpartAccountId)
            .currencyCode(amount.getCurrency().getCurrencyCode())
            .moneyAmount(amount.getNumberStripped())
            .createDateTimeUtc(Instant.now(Clock.systemUTC()))
            .build());
    }

    private void addMoney(MoneyAccountEntity account, Money amount, UUID userId,
                          String transactionCode, String counterpartAccountId) {
        Money newBalance = account.getBalance().add(amount);
        account.setBalanceAmount(newBalance.getNumberStripped());
        moneyAccountRepository.save(account);
        transactionLogRepository.save(TransactionLogEntity.builder()
            .operatingAccountId(account.getId())
            .operation("ADD")
            .operatorUserId(userId)
            .referenceCode(transactionCode)
            .counterpartAccountId(counterpartAccountId)
            .currencyCode(amount.getCurrency().getCurrencyCode())
            .moneyAmount(amount.getNumberStripped())
            .createDateTimeUtc(Instant.now(Clock.systemUTC()))
            .build());
    }

    @Override
    public List<TransactionLog> getAllTransactionLog(UUID userId) {
        return transactionLogRepository.findAllByOperatorUserIdOrderByCreateDateTimeUtcDesc(userId)
            .stream()
            .map(mapper::entityToDomainObject)
            .toList();
    }

}
