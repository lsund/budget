CREATE TABLE Category
(
    id              SERIAL PRIMARY KEY,
    label           varchar(64) NOT NULL,
    hidden          BOOLEAN NOT NULL,
    balance         INT NOT NULL,
    start_balance   INT NOT NULL,
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


CREATE TABLE AssetTransaction
(
    id          SERIAL PRIMARY KEY,
    assetid     INT NOT NULL,
    day         DATE NOT NULL,
    acc         VARCHAR(64) NOT NULL,
    buy         BOOLEAN NOT NULL,
    shares      INT NOT NULL,
    rate        FLOAT NOT NULL,
    currency    VARCHAR(16) NOT NULL,
    total       FLOAT NOT NULL,

    FOREIGN KEY (assetid) REFERENCES Asset (id)
);

CREATE TABLE Asset
(
    id          SERIAL PRIMARY KEY,
    label       VARCHAR(64) NOT NULL,
    tag         VARCHAR(16) NOT NULL,
    type        INT NOT NULL
);


CREATE TABLE Report
(
    id          SERIAL PRIMARY KEY,
    day         DATE NOT NULL,
    file        TEXT
);


CREATE TABLE Debt
(
    id          SERIAL PRIMARY KEY,
    label       TEXT NOT NULL,
    amount      INT NOT NULL
);
