package com.acmebank.acmeaccountmanager.service.impl;

import com.acmebank.acmeaccountmanager.service.api.AccountManagement;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import com.acmebank.acmeaccountmanager.service.impl.mapper.AccountManagementImplMapper;
import jakarta.persistence.EntityNotFoundException;
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

    public AccountManagementImpl(
        MoneyAccountRepository moneyAccountRepository,
        AccountManagementImplMapper mapper
    ) {
        this.moneyAccountRepository = moneyAccountRepository;
        this.mapper = mapper;
    }

    @Override
    public MoneyAccount getAccount(GetMoneyAccountRequest request) {
        final String moneyAccountId = request.id();
        final UUID userId = request.userId();   // TODO: authorization checking

        MoneyAccountEntity moneyAccountEntity = moneyAccountRepository.findById(moneyAccountId)
            .orElseThrow(
                () -> new EntityNotFoundException("MoneyAccount[%s] does not exist!".formatted(moneyAccountId)));
        return mapper.entityToDomainObject(moneyAccountEntity);
    }

    @Override
    public List<MoneyAccount> getAllAccounts(UUID userId) {
        return moneyAccountRepository.findAllByPrimaryOwnerIdOrderById(userId)
            .stream().map(mapper::entityToDomainObject)
            .toList();
    }
}
