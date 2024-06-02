package com.acmebank.acmeaccountmanager.service.exception;

public class InsufficientBalanceErrorException extends RuntimeException {
    public InsufficientBalanceErrorException(String accountId) {
        super("Account[%s] does not have enough balance!".formatted(accountId));
    }
}
