CREATE KEYSPACE IF NOT EXISTS kafka WITH replication = {'class': 'SimpleStrategy', 'replication_factor': '1'};

CREATE TABLE kafka.players (
    id text PRIMARY KEY,
    dob text,
    club text,
    foot text,
    name text,
    nationality text,
    number int,
    overall int,
    position text,
    value text
);

CREATE INDEX players_name ON kafka.players(name);

