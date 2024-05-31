package com.acmebank.acmeaccountmanager.service.impl.mapper;

import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import com.acmebank.acmeaccountmanager.service.impl.MoneyAccountEntity;
import org.javamoney.moneta.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AccountManagementImplMapper {

    @Mapping(target = "balance", source = ".", qualifiedByName = "toMoney")
    public abstract MoneyAccount entityToDomainObject(MoneyAccountEntity moneyAccountEntity);

    @Named("toMoney")
    public Money toMoney(MoneyAccountEntity entity) {
        return Money.of(entity.getBalanceAmount(), entity.getCurrencyCode());
    }
}
