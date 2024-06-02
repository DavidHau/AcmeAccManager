package com.acmebank.acmeaccountmanager.rest;

import com.acmebank.acmeaccountmanager.rest.mapper.AccountManagementMapper;
import com.acmebank.acmeaccountmanager.service.api.AccountManagement;
import com.acmebank.acmeaccountmanager.service.api.MoneyAccount;
import com.acmebank.acmeaccountmanager.service.api.TransactionLog;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RequestMapping("/accounts")
@RestController
public class MoneyAccountController {

    private final AccountManagement accountManagement;
    private final AccountManagementMapper mapper;


    public MoneyAccountController(
        AccountManagement accountManagement,
        AccountManagementMapper mapper
    ) {
        this.accountManagement = accountManagement;
        this.mapper = mapper;
    }

    @GetMapping("/{account-id}")
    @Operation(summary = "Get Money Account.",
        description = "To simplify stuff, only account owner user is permitted to retrieve the account.")
    public MoneyAccountVo getMoneyAccount(
        @RequestHeader
        @Parameter(schema = @Schema(example = "2a31b993-4895-4484-9521-066f741c89b9"))
        UUID userId,
        @PathVariable("account-id") String accountId
    ) {
        MoneyAccount account = accountManagement.getAccount(AccountManagement.GetMoneyAccountRequest.builder()
            .userId(userId)
            .id(accountId)
            .build());
        return mapper.serviceToRest(account);
    }

    @GetMapping
    @Operation(summary = "Get All Money Accounts.")
    public List<MoneyAccountVo> getAllMoneyAccounts(
        @RequestHeader
        @Parameter(schema = @Schema(example = "2a31b993-4895-4484-9521-066f741c89b9"))
        UUID userId
    ) {
        List<MoneyAccount> accounts = accountManagement.getAllAccounts(userId);
        return accounts.stream().map(mapper::serviceToRest)
            .toList();
    }

    @PostMapping("/{account-id}/transfer")
    @Operation(summary = "Transfer Money to Another Account.")
    public ResponseEntity<Void> transferMoneyToAnotherAccount(
        @RequestHeader
        @Parameter(schema = @Schema(example = "2a31b993-4895-4484-9521-066f741c89b9"))
        UUID userId,
        @PathVariable("account-id") String operatingAccountId,
        @RequestBody TransferMoneyToAnotherAccountRequestVo requestVo
    ) {
        final Integer operatingAccountVersion = requestVo.operatingAccountVersion();
        final String recipientAccountId = requestVo.recipientAccountId();
        final String currencyCode = requestVo.currencyCode();
        final BigDecimal toBeTransferAmount = requestVo.amount();

        accountManagement.transferMoneyToAccount(AccountManagement.TransferMoneyToAccountRequest.builder()
            .operatingAccountId(operatingAccountId)
            .operatingAccountVersion(operatingAccountVersion)
            .recipientAccountId(recipientAccountId)
            .currencyCode(currencyCode)
            .toBeTransferAmount(toBeTransferAmount)
            .userId(userId)
            .build());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transaction-log")
    @Operation(summary = "Get All Transaction Log by User.")
    public List<TransactionLog> getAllTransactionLogByUser(
        @RequestHeader
        @Parameter(schema = @Schema(example = "2a31b993-4895-4484-9521-066f741c89b9"))
        UUID userId
    ) {
        return accountManagement.getAllTransactionLog(userId);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MoneyAccountVo(
        @JsonProperty(required = true)
        String id,
        @JsonProperty(required = true)
        Integer version,
        @JsonProperty(required = true)
        UUID primaryOwnerId,
        @JsonProperty(required = true)
        String currencyCode,
        @JsonProperty(required = true)
        BigDecimal balanceAmount
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TransferMoneyToAnotherAccountRequestVo(
        @JsonProperty(required = true)
        Integer operatingAccountVersion,
        @JsonProperty(required = true)
        String recipientAccountId,
        @JsonProperty(required = true)
        String currencyCode,
        @JsonProperty(required = true)
        BigDecimal amount
    ) {
    }
}
