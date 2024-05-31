CREATE TABLE money_account
(
    id               VARCHAR(255)   NOT NULL,
    version          INT,
    primary_owner_id UUID           NOT NULL,
    currency_code    VARCHAR(255)   NOT NULL,
    balance_amount   DECIMAL(22, 2) NOT NULL,
    CONSTRAINT pk_money_account PRIMARY KEY (id)
);
