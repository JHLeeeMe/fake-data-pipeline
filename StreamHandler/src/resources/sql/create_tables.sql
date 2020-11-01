DROP TABLE IF EXISTS iot_tb;
DROP TABLE IF EXISTS words_tb;

CREATE TABLE IF NOT EXISTS iot_tb (
    device VARCHAR (10),
    temp FLOAT (8),
    humd FLOAT (8),
    pres FLOAT (8),
    created_time TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS words_tb (
    author VARCHAR (30),
    text TEXT,
    words INT,
    length INT,
    created_time TIMESTAMPTZ DEFAULT NOW()
);
