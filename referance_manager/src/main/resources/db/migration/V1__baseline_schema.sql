-- FIX: DEF-015 — referance_manager baseline schema (MySQL)

CREATE TABLE IF NOT EXISTS airlines (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    name       VARCHAR(255) NOT NULL,
    code       VARCHAR(2)  NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version    BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_airline_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS stations (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    icao_code  VARCHAR(4)   NOT NULL,
    name       VARCHAR(255) NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_station_icao UNIQUE (icao_code)
);

CREATE TABLE IF NOT EXISTS aircrafts (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    type        VARCHAR(255) NOT NULL,
    tail_number VARCHAR(255) NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_aircraft_tail UNIQUE (tail_number)
);

CREATE TABLE IF NOT EXISTS routes (
    id                     BIGINT NOT NULL AUTO_INCREMENT,
    origin_station_id      BIGINT NOT NULL,
    destination_station_id BIGINT NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    version                BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_route_origin_destination UNIQUE (origin_station_id, destination_station_id),
    CONSTRAINT fk_route_origin      FOREIGN KEY (origin_station_id)      REFERENCES stations(id),
    CONSTRAINT fk_route_destination FOREIGN KEY (destination_station_id) REFERENCES stations(id)
);
