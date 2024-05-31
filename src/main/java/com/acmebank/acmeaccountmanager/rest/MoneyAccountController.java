package com.acmebank.acmeaccountmanager.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RequestMapping("/account")
@RestController
public class MoneyAccountController {
    @GetMapping("/{account-id}")
    @Operation(summary = "Get Money Account.",
        description = "To simplify stuff, only account owner user is permitted to retrieve the account.")
    public MoneyAccountVo getMoneyAccount(
        @RequestHeader
        @Parameter(schema = @Schema(example = "2a31b993-4895-4484-9521-066f741c89b9"))
        UUID userId,
        @PathVariable("account-id") String accountId
    ) {
        return new MoneyAccountVo(
            accountId,
            1,
            userId,
            "HKD",
            BigDecimal.valueOf(1000000)
        );
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
}
