-- =====================================================================
--  Фотостудия: создание таблиц и начальных тестовых данных (PostgreSQL)
--  Перед запуском создайте пустую базу данных:  CREATE DATABASE photostudio;
--  Затем выполните этот скрипт, подключившись к базе photostudio.
--  Скрипт можно запускать повторно: старые таблицы будут пересозданы.
-- =====================================================================

DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS halls    CASCADE;
DROP TABLE IF EXISTS clients  CASCADE;

-- ---------------------------------------------------------------------
-- Клиенты (пользователи системы)
-- ---------------------------------------------------------------------
CREATE TABLE clients (
    id            SERIAL       PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    email         VARCHAR(100) NOT NULL UNIQUE,
    registered_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ---------------------------------------------------------------------
-- Залы студии
-- ---------------------------------------------------------------------
CREATE TABLE halls (
    id             SERIAL        PRIMARY KEY,
    name           VARCHAR(50)   NOT NULL UNIQUE,
    description    VARCHAR(255),
    capacity       INT           NOT NULL CHECK (capacity > 0),
    price_per_hour NUMERIC(10,2) NOT NULL CHECK (price_per_hour > 0)
);

-- ---------------------------------------------------------------------
-- Бронирования фотосессий (основная сущность)
--   bookings.client_id -> clients.id
--   bookings.hall_id   -> halls.id
-- ---------------------------------------------------------------------
CREATE TABLE bookings (
    id             SERIAL        PRIMARY KEY,
    client_id      INT           NOT NULL REFERENCES clients (id),
    hall_id        INT           NOT NULL REFERENCES halls (id),
    title          VARCHAR(100)  NOT NULL,
    description    VARCHAR(255),
    session_type   VARCHAR(20)   NOT NULL
                   CHECK (session_type IN ('PORTRAIT', 'FAMILY', 'CHILDREN', 'LOVE_STORY', 'PRODUCT', 'CORPORATE')),
    start_time     TIMESTAMP     NOT NULL,
    duration_hours INT           NOT NULL CHECK (duration_hours BETWEEN 1 AND 8),
    price          NUMERIC(10,2) NOT NULL CHECK (price >= 0),
    status         VARCHAR(20)   NOT NULL DEFAULT 'CREATED'
                   CHECK (status IN ('CREATED', 'CONFIRMED', 'COMPLETED', 'CANCELLED')),
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bookings_client ON bookings (client_id);
CREATE INDEX idx_bookings_hall   ON bookings (hall_id);
CREATE INDEX idx_bookings_start  ON bookings (start_time);

-- =====================================================================
--  Начальные тестовые данные
-- =====================================================================

INSERT INTO clients (full_name, phone, email) VALUES
    ('Иванова Анна Сергеевна',    '+79161234567', 'anna.ivanova@mail.ru'),
    ('Петров Дмитрий Алексеевич', '+79037654321', 'd.petrov@gmail.com'),
    ('Смирнова Елена Викторовна', '+79265551122', 'elena.smirnova@yandex.ru'),
    ('Козлов Максим Игоревич',    '+79857773344', 'm.kozlov@mail.ru'),
    ('Новикова Ольга Павловна',   '+79169990011', 'olga.novikova@gmail.com'),
    ('Морозов Артём Денисович',   '+79031112233', 'artem.morozov@yandex.ru');

INSERT INTO halls (name, description, capacity, price_per_hour) VALUES
    ('Loft',        'Светлый лофт с большими окнами и кирпичной стеной',      10, 1500.00),
    ('Циклорама',   'Белая циклорама для каталожной и предметной съёмки',      6, 1200.00),
    ('Dark Room',   'Тёмная студия с цветным светом и дым-машиной',            5, 1800.00),
    ('Green Garden','Зал с живыми растениями и естественным светом',          12, 2000.00);

-- Стоимость уже рассчитана по правилам студии:
--   выходные (сб, вс) = ставка зала x часы x 1.2;
--   съёмка от 4 часов в будни = ставка зала x часы x 0.9; иначе ставка x часы.
INSERT INTO bookings (client_id, hall_id, title, description, session_type, start_time, duration_hours, price, status) VALUES
    (1, 1, 'Портретная съёмка Анны',              'Портфолио для личного бренда',              'PORTRAIT',   '2026-08-25 10:00', 2,  3000.00, 'COMPLETED'),
    (2, 2, 'Семейная фотосессия Петровых',        'Семья из 4 человек, светлые образы',        'FAMILY',     '2026-08-29 12:00', 3,  4320.00, 'COMPLETED'),
    (3, 3, 'Love story Елены и Андрея',           'Съёмка в цветном свете',                    'LOVE_STORY', '2026-09-02 14:00', 2,  3600.00, 'COMPLETED'),
    (4, 4, 'Корпоративные портреты команды',      'Деловые портреты сотрудников для сайта',    'CORPORATE',  '2026-09-05 11:00', 5, 12000.00, 'COMPLETED'),
    (5, 2, 'Каталожная съёмка украшений',         'Предметная съёмка коллекции колец',         'PRODUCT',    '2026-09-09 16:00', 4,  4320.00, 'COMPLETED'),
    (6, 1, 'Детская фотосессия ко дню рождения',  'Съёмка именинника с шарами',                'CHILDREN',   '2026-09-12 10:00', 2,  3600.00, 'CANCELLED'),
    (1, 3, 'Творческий портрет в цветном свете',  'Художественный портрет для выставки',       'PORTRAIT',   '2026-09-18 15:00', 2,  3600.00, 'CONFIRMED'),
    (2, 1, 'Деловой портрет для профиля',         'Фото для LinkedIn и резюме',                'PORTRAIT',   '2026-09-22 13:00', 1,  1500.00, 'CONFIRMED'),
    (3, 4, 'Семейная съёмка в зелёном зале',      'Двое взрослых и двое детей',                'FAMILY',     '2026-09-26 12:00', 3,  7200.00, 'CONFIRMED'),
    (4, 2, 'Предметная съёмка одежды',            'Лукбук новой коллекции на манекене',        'PRODUCT',    '2026-09-28 11:00', 6,  6480.00, 'CREATED'),
    (5, 3, 'Фотосессия для двоих',                'Романтическая съёмка к годовщине',          'LOVE_STORY', '2026-10-01 17:00', 3,  5400.00, 'CREATED'),
    (1, 1, 'Newborn-съёмка малыша',               'Съёмка новорождённого с родителями',        'CHILDREN',   '2026-10-03 10:00', 2,  3600.00, 'CREATED');
