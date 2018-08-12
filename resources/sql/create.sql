CREATE TABLE Category
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL,
    funds           INT NOT NULL,
    monthly_limit   INT NOT NULL,
    spent           INT NOT NULL
);

CREATE TABLE Transaction
(
    id          SERIAL PRIMARY KEY,
    categoryid  INT NOT NULL,
    amount      INT NOT NULL,
    ts          TIMESTAMP,

    FOREIGN KEY (categoryid) REFERENCES Category (id)
);


/* Courtage for a transaction can be calculated by rate * shares - total */


CREATE TABLE StockTransaction
(
    id          SERIAL PRIMARY KEY,
    day         DATE NOT NULL,
    name        VARCHAR(64) NOT NULL,
    shortname   VARCHAR(16) NOT NULL,
    buy         BOOLEAN NOT NULL,
    shares      INT NOT NULL,
    rate        FLOAT NOT NULL,
    currency    VARCHAR(16) NOT NULL,
    total       FLOAT NOT NULL,
    courtage    FLOAT NOT NULL
);


CREATE TABLE FundTransaction
(
    id          SERIAL PRIMARY KEY,
    day         DATE NOT NULL,
    name        VARCHAR(64) NOT NULL,
    shortname   VARCHAR(16) NOT NULL,
    acc         VARCHAR(64) NOT NULL,
    buy         BOOLEAN NOT NULL,
    shares      INT NOT NULL,
    rate        FLOAT NOT NULL,
    currency    VARCHAR(16) NOT NULL,
    total       FLOAT NOT NULL
);
