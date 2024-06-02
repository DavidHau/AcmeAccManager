package com.acmebank.acmeaccountmanager.rest;

import com.acmebank.acmeaccountmanager.service.impl.MoneyAccountEntity;
import org.hamcrest.Matchers;
import org.javamoney.moneta.Money;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
class MoneyAccountControllerDbIntegrationTest {
    @Autowired
    private MockMvc mvc;

    final static String HEADER_USER_ID = "userId";

    @Autowired
    private CrudRepository<MoneyAccountEntity, String> moneyAccountRepositoryRaw;

    @BeforeAll
    void setup() {
        moneyAccountRepositoryRaw.deleteAll();
    }

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
        setupAccount(userId, accountId, Money.of(BigDecimal.valueOf(1_000_000.01), "HKD"));

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
                jsonPath("$.balanceAmount").value(1_000_000.01)
            );
    }

    @Test
    void shouldAllMoneyAccountsByUserOrderByAccountId() throws Exception {
        // given
        final UUID userId = UUID.randomUUID();
        final String accountId1 = "88888888" + UUID.randomUUID();
        final String accountId2 = "12345678" + UUID.randomUUID();
        setupAccount(userId, accountId1, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(userId, accountId2, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));

        // when
        mvc.perform(MockMvcRequestBuilders.get("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, userId)
            )

            // then
            .andExpectAll(status().isOk(),
                jsonPath("$").isArray(),
                jsonPath("$", hasSize(2)),

                jsonPath("$[0].id").value(Matchers.startsWith("12345678")),
                jsonPath("$[0].version").value(1),
                jsonPath("$[0].primaryOwnerId").value(userId.toString()),
                jsonPath("$[0].currencyCode").value("HKD"),
                jsonPath("$[0].balanceAmount").value(1_000_000),

                jsonPath("$[1].id").value(Matchers.startsWith("88888888")),
                jsonPath("$[1].version").value(1),
                jsonPath("$[1].primaryOwnerId").value(userId.toString()),
                jsonPath("$[1].currencyCode").value("HKD"),
                jsonPath("$[1].balanceAmount").value(1_000_000)
            );
    }

    @Test
    void shouldReturn404NotFoundWhenGetAccountGivenAccountNotExist() throws Exception {
        // given
        final UUID userId = UUID.randomUUID();
        final String nonExistingMoneyAccountId = UUID.randomUUID().toString();

        // when
        mvc.perform(MockMvcRequestBuilders.get("/accounts/{account-id}", nonExistingMoneyAccountId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, userId)
            )

            // then
            .andExpectAll(status().isNotFound(),
                jsonPath("$.error").value(Matchers.matchesRegex("MoneyAccount.* does not exist!"))
            );
    }

    @Test
    void shouldReturn403ForbiddenWhenGetAccountByNonOwner() throws Exception {
        // given
        final UUID accountOwnerUserId = UUID.randomUUID();
        final UUID nonAuthorizedUserId = UUID.randomUUID();
        final String accountId = "12345678" + UUID.randomUUID();
        setupAccount(accountOwnerUserId, accountId, Money.of(BigDecimal.valueOf(1_000_000.01), "HKD"));

        // when
        mvc.perform(MockMvcRequestBuilders.get("/accounts/{account-id}", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, nonAuthorizedUserId)
            )

            // then
            .andExpectAll(status().isForbidden(),
                jsonPath("$.error").value("You are not authorized!")
            );
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -0.1, -10000})
    void shouldNotTransferNegativeAmountToAnotherAccount(double toBeTransferredAmount) throws Exception {
        final UUID accountOwnerUserId = UUID.randomUUID();
        final String accountId1 = "12345678" + UUID.randomUUID();
        final String accountId2 = "88888888" + UUID.randomUUID();

        mvc.perform(MockMvcRequestBuilders.post("/accounts/{account-id}/transfer", accountId1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, accountOwnerUserId)
                .content("""
                        {
                            "operatingAccountVersion": 1,
                            "recipientAccountId": "%s",
                            "currencyCode": "HKD",
                            "amount": %s
                        }
                    """.formatted(accountId2, toBeTransferredAmount))
            )
            .andExpectAll(status().isBadRequest(),
                jsonPath("$.error").value(Matchers.endsWith("must be greater than 0")));
    }

    @Test
    void shouldBeAbleToTransferMoneyToAnotherAccount() throws Exception {
        // given
        final UUID accountOwnerUserId = UUID.randomUUID();
        final String accountId1 = "12345678" + UUID.randomUUID();
        final String accountId2 = "88888888" + UUID.randomUUID();
        setupAccount(accountOwnerUserId, accountId1, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(accountOwnerUserId, accountId2, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));

        // when
        mvc.perform(MockMvcRequestBuilders.post("/accounts/{account-id}/transfer", accountId1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, accountOwnerUserId)
                .content("""
                        {
                            "operatingAccountVersion": 1,
                            "recipientAccountId": "%s",
                            "currencyCode": "HKD",
                            "amount": 10000
                        }
                    """.formatted(accountId2))
            )

            // then
            .andExpectAll(status().isNoContent());

        mvc.perform(MockMvcRequestBuilders.get("/accounts")
            .contentType(MediaType.APPLICATION_JSON)
            .header(HEADER_USER_ID, accountOwnerUserId)
        ).andExpectAll(status().isOk(),
            jsonPath("$").isArray(),
            jsonPath("$", hasSize(2)),

            jsonPath("$[0].id").value(Matchers.startsWith("12345678")),
            jsonPath("$[0].version").value(2),
            jsonPath("$[0].currencyCode").value("HKD"),
            jsonPath("$[0].balanceAmount").value(990_000),

            jsonPath("$[1].id").value(Matchers.startsWith("88888888")),
            jsonPath("$[1].version").value(2),
            jsonPath("$[1].currencyCode").value("HKD"),
            jsonPath("$[1].balanceAmount").value(1_010_000)
        );
    }

    @Test
    void shouldReturn422UnprocessableEntityWhenTransferMoneyToAnotherAccountWithMismatchedCurrency() throws Exception {
        // given
        final UUID accountOwnerUserId = UUID.randomUUID();
        final String accountId1 = "12345678" + UUID.randomUUID();
        final String accountId2 = "88888888" + UUID.randomUUID();
        setupAccount(accountOwnerUserId, accountId1, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(accountOwnerUserId, accountId2, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));

        // when
        mvc.perform(MockMvcRequestBuilders.post("/accounts/{account-id}/transfer", accountId1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, accountOwnerUserId)
                .content("""
                        {
                            "operatingAccountVersion": 1,
                            "recipientAccountId": "%s",
                            "currencyCode": "USD",
                            "amount": 10000
                        }
                    """.formatted(accountId2))
            )

            // then
            .andExpectAll(status().isUnprocessableEntity(),
                jsonPath("$.error").value(Matchers.startsWith("Currency mismatch:")));
    }

    @Test
    void shouldReturn422UnprocessableEntityWhenTransferMoneyToAnotherAccountWithInsufficientBalance() throws Exception {
        // given
        final UUID accountOwnerUserId = UUID.randomUUID();
        final String accountId1 = "12345678" + UUID.randomUUID();
        final String accountId2 = "88888888" + UUID.randomUUID();
        setupAccount(accountOwnerUserId, accountId1, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(accountOwnerUserId, accountId2, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));

        // when
        mvc.perform(MockMvcRequestBuilders.post("/accounts/{account-id}/transfer", accountId1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, accountOwnerUserId)
                .content("""
                        {
                            "operatingAccountVersion": 1,
                            "recipientAccountId": "%s",
                            "currencyCode": "HKD",
                            "amount": 1000000.01
                        }
                    """.formatted(accountId2))
            )

            // then
            .andExpectAll(status().isUnprocessableEntity(),
                jsonPath("$.error").value("Account[%s] does not have enough balance!".formatted(accountId1)));
    }

    @Test
    void shouldReturn409ConflictWhenTransferMoneyToAnotherAccountGivenStaledOperatingAccountData() throws Exception {
        // given
        final UUID accountOwnerUserId = UUID.randomUUID();
        final String accountId1 = "12345678" + UUID.randomUUID();
        final String accountId2 = "88888888" + UUID.randomUUID();
        setupAccount(accountOwnerUserId, accountId1, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(accountOwnerUserId, accountId2, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        // 1st update
        mvc.perform(MockMvcRequestBuilders.post("/accounts/{account-id}/transfer", accountId1)
            .contentType(MediaType.APPLICATION_JSON)
            .header(HEADER_USER_ID, accountOwnerUserId)
            .content("""
                    {
                        "operatingAccountVersion": 1,
                        "recipientAccountId": "%s",
                        "currencyCode": "HKD",
                        "amount": 10000
                    }
                """.formatted(accountId2))
        ).andExpectAll(status().isNoContent());

        // when using staled data(version) to update
        mvc.perform(MockMvcRequestBuilders.post("/accounts/{account-id}/transfer", accountId1)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, accountOwnerUserId)
                .content("""
                        {
                            "operatingAccountVersion": 1,
                            "recipientAccountId": "%s",
                            "currencyCode": "HKD",
                            "amount": 10000
                        }
                    """.formatted(accountId2))
            )

            // then
            .andExpectAll(status().isConflict(),
                jsonPath("$.error").value("Concurrent operation conflict is detected."));
    }

    @Test
    void shouldReturn403ForbiddenWhenTransferMoneyToAnotherAccountByNonAccountOwner() throws Exception {
        // given
        final UUID accountOwnerUserId = UUID.randomUUID();
        final UUID anotherAccountOwnerUserId = UUID.randomUUID();
        final String accountId = "12345678" + UUID.randomUUID();
        final String anotherAccountId = "88888888" + UUID.randomUUID();
        setupAccount(accountOwnerUserId, accountId, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));
        setupAccount(anotherAccountOwnerUserId, anotherAccountId, Money.of(BigDecimal.valueOf(1_000_000), "HKD"));

        // when
        mvc.perform(MockMvcRequestBuilders.post("/accounts/{account-id}/transfer", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_USER_ID, anotherAccountOwnerUserId)
                .content("""
                        {
                            "operatingAccountVersion": 1,
                            "recipientAccountId": "%s",
                            "currencyCode": "HKD",
                            "amount": 10000
                        }
                    """.formatted(anotherAccountId))
            )

            // then
            .andExpectAll(status().isForbidden(),
                jsonPath("$.error").value("You are not authorized!"));
    }

}