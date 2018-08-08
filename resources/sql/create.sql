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
