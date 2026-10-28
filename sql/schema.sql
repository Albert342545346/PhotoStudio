-- ============================================================
-- Фотостудия: схема базы данных (PostgreSQL)
-- Перед запуском создайте базу:  CREATE DATABASE photostudio ENCODING 'UTF8';
-- Запуск вручную:  psql -U postgres -d photostudio -f sql/schema.sql
-- ВНИМАНИЕ: скрипт пересоздаёт таблицы (все данные удаляются).
-- ============================================================

DROP TABLE IF EXISTS payments CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS halls CASCADE;
DROP TABLE IF EXISTS photographers CASCADE;
DROP TABLE IF EXISTS clients CASCADE;

CREATE TABLE clients (
    id            BIGSERIAL    PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    vip           BOOLEAN      NOT NULL DEFAULT FALSE,
    registered_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_clients_phone UNIQUE (phone),
    CONSTRAINT uq_clients_email UNIQUE (email),
    CONSTRAINT chk_clients_name CHECK (LENGTH(TRIM(full_name)) > 0)
);

CREATE TABLE photographers (
    id             BIGSERIAL     PRIMARY KEY,
    full_name      VARCHAR(100)  NOT NULL,
    phone          VARCHAR(20)   NOT NULL,
    email          VARCHAR(100)  NOT NULL,
    specialization VARCHAR(20)   NOT NULL,
    hourly_rate    NUMERIC(10,2) NOT NULL,
    active         BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_photographers_phone UNIQUE (phone),
    CONSTRAINT uq_photographers_email UNIQUE (email),
    CONSTRAINT chk_photographers_name CHECK (LENGTH(TRIM(full_name)) > 0),
    CONSTRAINT chk_photographers_rate CHECK (hourly_rate > 0),
    CONSTRAINT chk_photographers_spec CHECK (specialization IN
        ('PORTRAIT', 'FAMILY', 'LOVE_STORY', 'PRODUCT', 'CORPORATE', 'WEDDING'))
);

CREATE TABLE halls (
    id           BIGSERIAL     PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    capacity     INTEGER       NOT NULL,
    hourly_price NUMERIC(10,2) NOT NULL,
    description  VARCHAR(255),
    CONSTRAINT uq_halls_name UNIQUE (name),
    CONSTRAINT chk_halls_capacity CHECK (capacity > 0),
    CONSTRAINT chk_halls_price CHECK (hourly_price >= 0)
);

CREATE TABLE bookings (
    id              BIGSERIAL     PRIMARY KEY,
    client_id       BIGINT        NOT NULL,
    photographer_id BIGINT        NOT NULL,
    hall_id         BIGINT        NOT NULL,
    session_type    VARCHAR(20)   NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'CREATED',
    start_time      TIMESTAMP     NOT NULL,
    end_time        TIMESTAMP     NOT NULL,
    participants    INTEGER       NOT NULL,
    total_price     NUMERIC(12,2) NOT NULL,
    comment         VARCHAR(255),
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bookings_client FOREIGN KEY (client_id) REFERENCES clients (id),
    CONSTRAINT fk_bookings_photographer FOREIGN KEY (photographer_id) REFERENCES photographers (id),
    CONSTRAINT fk_bookings_hall FOREIGN KEY (hall_id) REFERENCES halls (id),
    CONSTRAINT chk_bookings_type CHECK (session_type IN
        ('PORTRAIT', 'FAMILY', 'LOVE_STORY', 'PRODUCT', 'CORPORATE', 'WEDDING')),
    CONSTRAINT chk_bookings_status CHECK (status IN ('CREATED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_bookings_time CHECK (end_time > start_time),
    CONSTRAINT chk_bookings_participants CHECK (participants > 0),
    CONSTRAINT chk_bookings_price CHECK (total_price >= 0)
);

CREATE TABLE payments (
    id         BIGSERIAL     PRIMARY KEY,
    booking_id BIGINT        NOT NULL,
    amount     NUMERIC(12,2) NOT NULL,
    method     VARCHAR(20)   NOT NULL,
    paid_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0),
    CONSTRAINT chk_payments_method CHECK (method IN ('CARD', 'CASH', 'TRANSFER'))
);

CREATE INDEX idx_bookings_start_time ON bookings (start_time);
CREATE INDEX idx_bookings_status ON bookings (status);
CREATE INDEX idx_bookings_client ON bookings (client_id);
CREATE INDEX idx_payments_booking ON payments (booking_id);
