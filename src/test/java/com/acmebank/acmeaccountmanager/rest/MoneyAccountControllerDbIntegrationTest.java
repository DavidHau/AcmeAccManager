package com.acmebank.acmeaccountmanager.rest;

import com.acmebank.acmeaccountmanager.service.impl.MoneyAccountEntity;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MoneyAccountControllerDbIntegrationTest {
    @Autowired
    private MockMvc mvc;

    final static String HEADER_USER_ID = "userId";

    @Autowired
    private CrudRepository<MoneyAccountEntity, String> moneyAccountRepositoryRaw;

    void setupAccount(UUID userId, String accountId, Money balance) {
        MoneyAccountEntity accountEntity = MoneyAccountEntity.builder()
            .id(accountId)
            .version(1)
            .primaryOwnerId(userId)
            .currencyCode(balance.getCurrency().getCurrencyCode())
            .balanceAmount(balance.getNumberStripped())
            .build();
        moneyAccountRepositoryRaw.save(accountEntity);
    }

    @Test
    void shouldGetSingleMoneyAccount() throws Exception {
        // given
        final UUID userId = UUID.randomUUID();
        final String accountId = "12345678";
        setupAccount(userId, accountId, Money.of(BigDecimal.valueOf(1000000.01), "HKD"));

        // when
        mvc.perform(MockMvcRequestBuilders.get("/accounts/{account-id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, userId)
            )

            // then
            .andExpectAll(status().isOk(),
                jsonPath("$.id").value("12345678"),
                jsonPath("$.version").value(1),
                jsonPath("$.primaryOwnerId").value(userId.toString()),
                jsonPath("$.currencyCode").value("HKD"),
                jsonPath("$.balanceAmount").value(1000000.01)
            );
    }
}