package com.acmebank.acmeaccountmanager.service.impl;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "transaction_log")
public class TransactionLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String operatingAccountId;

    @Column(nullable = false)
    private String operation;

    @Column(nullable = false)
    private UUID operatingAccountUserId;

    @Column(nullable = false)
    private String referenceCode;

    @Column
    private String counterpartAccountId;

    @Column
    private String currencyCode;

    @Column
    private BigDecimal moneyAmount;

    @Column(nullable = false)
    Instant createDateTimeUtc;
}
