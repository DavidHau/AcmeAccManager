package com.acmebank.acmeaccountmanager.rest.exception;

import com.acmebank.acmeaccountmanager.service.exception.NotAuthorizedErrorException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.money.MonetaryException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Client Side Error

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> generateEntityNotFoundExceptionResponse(
        EntityNotFoundException exception) {
        log.info(exception.getMessage(), exception);
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
            HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> generateConstraintViolationExceptionResponse(
        ConstraintViolationException exception) {
        log.info(exception.getMessage(), exception);
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
            HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MonetaryException.class)
    public ResponseEntity<ErrorResponse> generateMonetaryExceptionResponse(
        MonetaryException exception) {
        log.info(exception.getMessage(), exception);
        return new ResponseEntity<>(new ErrorResponse(exception.getMessage()),
            HttpStatus.UNPROCESSABLE_ENTITY);
    }


    // ---- Auth Error ----

    @ExceptionHandler(NotAuthorizedErrorException.class)
    public ResponseEntity<ErrorResponse> generateNotAuthorizedErrorExceptionResponse(
        NotAuthorizedErrorException exception) {
        log.warn("NotAuthorizedErrorException: " + exception.getMessage());
        return new ResponseEntity<>(new ErrorResponse("You are not authorized!"), HttpStatus.FORBIDDEN);
    }


    // Server Side Error


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generateExceptionResponse(
        Exception exception) {
        log.error(exception.getMessage(), exception);
        return new ResponseEntity<>(new ErrorResponse("Unclassified error happened!"),
            HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
