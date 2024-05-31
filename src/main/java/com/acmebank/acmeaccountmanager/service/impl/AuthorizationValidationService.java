package com.acmebank.acmeaccountmanager.service.impl;

import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import com.acmebank.acmeaccountmanager.service.exception.NotAuthorizedErrorException;

import java.util.UUID;

class AuthorizationValidationService {


    public void ensureHasReadAccess(MoneyAccount moneyAccount, UUID userId) {
        if (!moneyAccount.primaryOwnerId().equals(userId)) {
            throw new NotAuthorizedErrorException("User[%s] is trying to read MoneyAccount[%s]"
                .formatted(userId, moneyAccount.id()));
        }
    }
}
