package com.acmebank.acmeaccountmanager.service.impl;

import com.acmebank.acmeaccountmanager.service.api.AccountManagement;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import com.acmebank.acmeaccountmanager.service.exception.InsufficientBalanceErrorException;
import com.acmebank.acmeaccountmanager.service.impl.mapper.AccountManagementImplMapper;
import jakarta.persistence.EntityNotFoundException;
import org.javamoney.moneta.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.UUID;

@Service
@Validated
@Transactional
class AccountManagementImpl implements AccountManagement {

    private final MoneyAccountRepository moneyAccountRepository;
    private final AccountManagementImplMapper mapper;
    private final AuthorizationValidationService authorizationValidationService;

    public AccountManagementImpl(
        MoneyAccountRepository moneyAccountRepository,
        AccountManagementImplMapper mapper
    ) {
        this.moneyAccountRepository = moneyAccountRepository;
        this.mapper = mapper;
        this.authorizationValidationService = new AuthorizationValidationService();
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
        final MoneyAccountEntity operatingAccount = getMoneyAccountEntityOrThrow(request.operatingAccountId());
        final Integer operatingAccountVersion = request.operatingAccountVersion();
        final MoneyAccountEntity recipientAccount = getMoneyAccountEntityOrThrow(request.recipientAccountId());
        Money toBeTransferMoney = Money.of(request.toBeTransferAmount(), request.currencyCode());
        // TODO: Authorization for operation account
        // TODO: concurrent edit protection
        // TODO: transaction log
        // TODO: transaction reference number
        deductMoney(operatingAccount, operatingAccountVersion, toBeTransferMoney);
        addMoney(recipientAccount, toBeTransferMoney);
    }

    private void deductMoney(MoneyAccountEntity account, int versionNumber, Money amount) {
        Money newBalance = account.getBalance().subtract(amount);
        if (newBalance.isNegative()) {
            throw new InsufficientBalanceErrorException(account.getId());
        }
        account.setBalanceAmount(newBalance.getNumberStripped());
        moneyAccountRepository.save(account);
    }

    private void addMoney(MoneyAccountEntity account, Money amount) {
        Money newBalance = account.getBalance().add(amount);
        account.setBalanceAmount(newBalance.getNumberStripped());
        moneyAccountRepository.save(account);
    }
}
