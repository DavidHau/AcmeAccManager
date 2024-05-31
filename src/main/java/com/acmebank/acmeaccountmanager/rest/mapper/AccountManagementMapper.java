package com.acmebank.acmeaccountmanager.rest.mapper;

import com.acmebank.acmeaccountmanager.rest.MoneyAccountController;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class AccountManagementMapper {

    @Mapping(target = "currencyCode", source = "balance.currency.currencyCode")
    @Mapping(target = "balanceAmount", source = "balance.numberStripped")
    public abstract MoneyAccountController.MoneyAccountVo serviceToRest(MoneyAccount moneyAccount);
}
