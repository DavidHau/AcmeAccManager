CREATE TABLE transaction_log
(
    id                     UUID         NOT NULL,
    operating_account_id   VARCHAR(255) NOT NULL,
    operation              VARCHAR(255) NOT NULL,
    operator_user_id       UUID         NOT NULL,
    reference_code         VARCHAR(255) NOT NULL,
    counterpart_account_id VARCHAR(255),
    currency_code          VARCHAR(255),
    money_amount           DECIMAL,
    create_date_time_utc   TIMESTAMP    NOT NULL,
    CONSTRAINT pk_transaction_log PRIMARY KEY (id)
);