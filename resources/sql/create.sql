CREATE TABLE Category
(
    id              SERIAL PRIMARY KEY,
    name            varchar(64) NOT NULL,
    balance         INT NOT NULL,
    limit           INT NOT NULL,
    spent           INT NOT NULL
);

CREATE TABLE Transaction
(
    id          SERIAL PRIMARY KEY,
    categoryid  INT NOT NULL,
    amount      INT NOT NULL,
    note        TEXT,
    ts          TIMESTAMP,

    FOREIGN KEY (categoryid) REFERENCES Category (id)
);


/* Courtage for a stock transaction can be calculated by rate * shares - total */


CREATE TABLE StockTransaction
(
    id          SERIAL PRIMARY KEY,
    stockid     INT NOT NULL,
    day         DATE NOT NULL,
    acc         VARCHAR(64) NOT NULL,
    buy         BOOLEAN NOT NULL,
    shares      INT NOT NULL,
    rate        FLOAT NOT NULL,
    currency    VARCHAR(16) NOT NULL,
    total       FLOAT NOT NULL,

    FOREIGN KEY (stockid) REFERENCES Stock (id)
);

CREATE TABLE Stock
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    shortname   VARCHAR(16) NOT NULL
);


CREATE TABLE FundTransaction
(
    id          SERIAL PRIMARY KEY,
    fundid      INT NOT NULL,
    day         DATE NOT NULL,
    acc         VARCHAR(64) NOT NULL,
    buy         BOOLEAN NOT NULL,
    shares      FLOAT NOT NULL,
    rate        FLOAT NOT NULL,
    currency    VARCHAR(16) NOT NULL,
    total       FLOAT NOT NULL,

    FOREIGN KEY (stockid) REFERENCES Stock (id)
);

CREATE TABLE Fund
(
    id          SERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    shortname   VARCHAR(16) NOT NULL
);

CREATE TABLE Report
(
    id          SERIAL PRIMARY KEY,
    day         DATE NOT NULL,
    file        TEXT
);
