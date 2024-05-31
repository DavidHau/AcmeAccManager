package com.acmebank.acmeaccountmanager.service.exception;

public class NotAuthorizedErrorException extends RuntimeException {
    public NotAuthorizedErrorException(String message) {
        super(message);
    }
}
