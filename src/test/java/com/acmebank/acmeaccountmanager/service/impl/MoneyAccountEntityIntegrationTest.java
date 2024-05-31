package com.acmebank.acmeaccountmanager.service.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MoneyAccountEntityIntegrationTest {
    @Autowired
    MoneyAccountRepository repository;

    @Test
    void shouldRejectInsertingNegativeAmountOfBalance() {
        MoneyAccountEntity dummyAccountEntity = MoneyAccountEntity.builder()
            .id(UUID.randomUUID().toString())
            .version(1)
            .primaryOwnerId(UUID.randomUUID())
            .currencyCode("HKD")
            .balance(BigDecimal.valueOf(-123))
            .build();

        Exception actualException = assertThrows(Exception.class,
            () -> repository.save(dummyAccountEntity));

        assertThat(actualException.getMessage()).isEqualTo("Account balance cannot be negative");
    }

}