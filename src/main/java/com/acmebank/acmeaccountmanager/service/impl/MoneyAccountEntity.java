package com.acmebank.acmeaccountmanager.service.impl;

import jakarta.persistence.*;
import lombok.*;
import org.javamoney.moneta.Money;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "money_account")
public class MoneyAccountEntity {
    @Id
    @Setter(AccessLevel.NONE)
    private String id;

    @Version
    private Integer version;

    @Setter(AccessLevel.NONE)   // account owner cannot be changed
    @Column(nullable = false)
    private UUID primaryOwnerId;

    @Setter(AccessLevel.NONE)   // account currency cannot be changed
    @Column(nullable = false)
    String currencyCode;

    @Column(precision = 22, scale = 2, nullable = false)
    private BigDecimal balanceAmount;

    @Transient
    public Money getBalance() {
        return Money.of(balanceAmount, currencyCode);
    }

    @PrePersist
    @PreUpdate
    private void validateBalance() {
        if (balanceAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Account balance cannot be negative");
        }
    }
}
