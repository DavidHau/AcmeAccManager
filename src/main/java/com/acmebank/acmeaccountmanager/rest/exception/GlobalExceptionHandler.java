package com.acmebank.acmeaccountmanager.rest.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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


    // Server Side Error


    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> generateExceptionResponse(
        Exception exception) {
        log.error(exception.getMessage(), exception);
        return new ResponseEntity<>(new ErrorResponse("Unclassified error happened!"),
            HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
