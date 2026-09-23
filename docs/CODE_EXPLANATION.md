# Объяснение кода для защиты КР 1 — «Фотостудия»

Документ написан так, чтобы каждый из 4 участников команды мог за 5 минут понять свою часть проекта и ответить на вопросы преподавателя.

**Содержание**

1. [Идея системы в двух словах](#1-идея-системы-в-двух-словах)
2. [Архитектура: 4 слоя](#2-архитектура-4-слоя)
3. [Как одна операция проходит через все слои](#3-как-одна-операция-проходит-через-все-слои-создание-брони)
4. [База данных](#4-база-данных)
5. [Пакет `model`](#5-пакет-model-предметная-модель)
6. [Пакет `repository`](#6-пакет-repository-jdbc)
7. [Пакет `service`](#7-пакет-service-бизнес-логика)
8. [Пакет `exception`](#8-пакет-exception)
9. [Пакет `ui`](#9-пакет-ui-консольные-меню)
10. [Пакет `util` и класс `Main`](#10-пакет-util-и-класс-main)
11. [Таблица: где в коде каждая «ключевая тема» защиты](#11-где-в-коде-каждая-ключевая-тема-защиты)
12. [Бизнес-правила (таблица)](#12-бизнес-правила)
13. [Вопросы преподавателя и готовые ответы](#13-вопросы-преподавателя-и-готовые-ответы)
14. [Как поделить рассказ между 4 участниками](#14-как-поделить-рассказ-между-4-участниками)

---

## 1. Идея системы в двух словах

Фотостудия сдаёт залы для фотосессий. В системе три сущности:

- **Client** — клиент студии (ФИО, телефон, email);
- **Hall** — зал (название, вместимость, цена за час);
- **Booking** — **основная сущность**: бронирование фотосессии (клиент + зал + время + тип съёмки + цена + статус).

Администратор через консольное меню создаёт клиентов и брони, меняет статусы (Создана → Подтверждена → Завершена / Отменена), ищет, фильтрует, сортирует, смотрит статистику и выгружает данные в Excel.

Связи: **один клиент — много броней**, **один зал — много броней** (см. `docs/er_diagram.png`).

---

## 2. Архитектура: 4 слоя

```
┌────────────────────────────────────────────────────────────┐
│ Console UI     ui/*           меню, ввод/вывод              │  «общается с человеком»
├────────────────────────────────────────────────────────────┤
│ Service        service/*      бизнес-правила, проверки      │  «думает»
├────────────────────────────────────────────────────────────┤
│ Repository     repository/*   SQL, JDBC                     │  «общается с БД»
├────────────────────────────────────────────────────────────┤
│ Database       PostgreSQL     хранение, ограничения         │
└────────────────────────────────────────────────────────────┘
        model/* — объекты (Client, Hall, Booking, enum), их «возят» между слоями
        exception/* — собственные ошибки;  util/* — подключение к БД, Excel, CSV
```

**Главные правила слоёв (их любят проверять):**

| Слой | Что МОЖНО | Что НЕЛЬЗЯ |
|---|---|---|
| UI | выводить меню, читать ввод, вызывать методы сервисов | писать SQL, проверять бизнес-правила |
| Service | проверять правила, считать цену, вызывать репозитории | использовать `Scanner`, `System.out`, писать SQL |
| Repository | SQL, `PreparedStatement`, `ResultSet` | бизнес-правила (кроме простого «не найдено») |

Проверка: во всём `ui/*` нет ни одного слова `SELECT`, `Connection`, `PreparedStatement`; а в `service/*` нет `Scanner` и `System.out`.

---

## 3. Как одна операция проходит через все слои (создание брони)

Пользователь выбирает **2 → 1** (Бронирования → Создать бронирование).

1. **`Main`** запустил `MainMenu.run()`. Пункт 2 вызвал `bookingMenu.run()`, пункт 1 — метод `BookingMenu.create()`.
2. **`BookingMenu.create()`** через `ConsoleInput` читает: ID клиента, ID зала, название, описание, тип (`enum`), дату-время, часы. Если ввод неверного формата (например, «abc» вместо часов) `ConsoleInput` бросает `InvalidInputException`.
3. `BookingMenu` вызывает **`bookingService.create(...)`** — передаёт уже готовые типы (`int`, `SessionType`, `LocalDateTime`), а не строки.
4. **`BookingService.create()`** последовательно проверяет правила:
   название → клиент существует → зал существует → длительность 1–8 → рабочее время 9:00–21:00 → не в прошлом → нет пересечения по залу.
   Любое нарушение — `throw new BusinessException("...")`.
5. Сервис считает **цену** — перебирает стратегии (`WeekendPricing`, `LongSessionPricing`, `StandardPricing`) и берёт первую подходящую.
6. Сервис создаёт объект `Booking` и вызывает **`bookingRepository.save(booking)`**.
7. **`BookingRepository.save()`** открывает соединение (`DatabaseManager.getConnection()`), готовит `PreparedStatement` с `INSERT ... VALUES (?, ?, ...)`, подставляет значения, выполняет, получает сгенерированный `id`.
8. Управление возвращается вверх. Сервис перечитывает бронь `getById(...)` (чтобы получить имя клиента и зала через `JOIN`) и отдаёт в меню.
9. Меню печатает `booking.describe()`.
10. Если на любом шаге вылетело исключение, его ловит **`Menu.run()`**:
    `catch (AppException e) → System.out.println("Ошибка: " + e.getMessage())` — и меню продолжает работать.

---

## 4. База данных

Файл: `sql/photostudio.sql`. Три таблицы:

```sql
clients  (id PK, full_name NOT NULL, phone NOT NULL, email NOT NULL UNIQUE, registered_at)
halls    (id PK, name NOT NULL UNIQUE, description, capacity CHECK>0, price_per_hour CHECK>0)
bookings (id PK,
          client_id FK -> clients.id,  hall_id FK -> halls.id,
          title NOT NULL, description, session_type CHECK IN (...),
          start_time NOT NULL, duration_hours CHECK BETWEEN 1 AND 8,
          price CHECK >= 0, status CHECK IN (...) DEFAULT 'CREATED', created_at)
```

Какие ограничения где применены:

| Ограничение | Пример |
|---|---|
| `PRIMARY KEY` | `id SERIAL PRIMARY KEY` — автоинкремент |
| `FOREIGN KEY` | `client_id INT NOT NULL REFERENCES clients (id)` — нельзя создать бронь на несуществующего клиента и нельзя удалить клиента с бронями (на уровне БД тоже) |
| `NOT NULL` | `title`, `start_time`, `price` … |
| `UNIQUE` | `clients.email`, `halls.name` |
| `CHECK` | `duration_hours BETWEEN 1 AND 8`, `status IN ('CREATED','CONFIRMED','COMPLETED','CANCELLED')` |

**Enum хранятся строками** (`'CONFIRMED'`, `'PORTRAIT'`): в БД — `VARCHAR` с `CHECK`, в Java — `status.name()` при записи и `BookingStatus.valueOf(...)` при чтении.

**Важная мысль:** правила защищены **дважды** — в Java (понятное сообщение пользователю) и в БД (последний рубеж, если кто-то обойдёт приложение).

---

## 5. Пакет `model` — предметная модель

### `BaseEntity` (абстрактный класс)
```java
public abstract class BaseEntity {
    private int id;                      // инкапсуляция: поле private
    public int getId() {...}  public void setId(int id) {...}
    public abstract String describe();   // каждый наследник описывает себя по-своему
    @Override public String toString() { return describe(); }
}
```
Общий предок для `Client`, `Hall`, `Booking`. Хранит `id`, требует реализовать `describe()`.

### `Client`, `Hall`, `Booking` (наследники `BaseEntity`)
- Все поля `private`, доступ через геттеры/сеттеры (**инкапсуляция**).
- У `Client` и `Booking` **два конструктора**: для нового объекта (без `id`) и для объекта, прочитанного из БД (с `id`, `createdAt`). Это удобно: `id` присваивает база данных.
- `Hall` неизменяемый (все поля `final`) — залы в этой КР только читаются.
- `Booking.getEndTime()` — вычисляемое значение (`startTime + durationHours`), в БД не хранится.
- `Booking.clientName` и `hallName` — «служебные» поля, заполняются в репозитории через `JOIN`, чтобы красиво печатать бронь без лишних запросов.

### `BookingStatus` (enum) — использование enum
```java
CREATED("Создана"), CONFIRMED("Подтверждена"), COMPLETED("Завершена"), CANCELLED("Отменена");

public boolean canTransitionTo(BookingStatus target) {
    return switch (this) {
        case CREATED   -> target == CONFIRMED || target == CANCELLED;
        case CONFIRMED -> target == COMPLETED || target == CANCELLED;
        case COMPLETED, CANCELLED -> false;
    };
}
```
Enum здесь — **не просто список констант**: у него есть русское название, метод допустимых переходов (бизнес-правило), методы `isFinal()`, `isActive()`.

Диаграмма переходов:
```
CREATED ──► CONFIRMED ──► COMPLETED
   │            │
   └────────────┴──────► CANCELLED
(COMPLETED и CANCELLED — конечные, из них выйти нельзя)
```

### `SessionType` (enum)
Тип съёмки: `PORTRAIT`, `FAMILY`, `CHILDREN`, `LOVE_STORY`, `PRODUCT`, `CORPORATE`.

### `BookingSort` (enum с поведением)
Каждое значение хранит свой `Comparator`:
```java
BY_PRICE_DESC("По стоимости (сначала дорогие)", Comparator.comparing(Booking::getPrice).reversed()),
```
Поэтому сортировка в сервисе — одна строка: `.sorted(sort.getComparator())`, а меню сортировки строится **автоматически** из `BookingSort.values()`. Добавили новое значение в enum — новый пункт меню появился сам.

### `Titled` (интерфейс)
```java
public interface Titled { String getTitle(); }
```
Реализуют все три enum. Благодаря ему в `ConsoleInput` есть **один** универсальный метод выбора значения из списка `readEnum(...)` для любого enum.

### `TableData` (record)
Простой контейнер «название таблицы + колонки + строки» для пункта «Вывести таблицы БД».

---

## 6. Пакет `repository` (JDBC)

### Интерфейсы
```java
public interface ReadRepository<T>  { Optional<T> findById(int id);  List<T> findAll(); }
public interface CrudRepository<T> extends ReadRepository<T> {
    T save(T entity);  void update(T entity);  boolean deleteById(int id);
}
```
- `BookingRepository`, `ClientRepository` реализуют **`CrudRepository`** (полный CRUD).
- `HallRepository` реализует только **`ReadRepository`** (залы только читаем) — это принцип «класс должен уметь ровно то, что ему нужно».
- `<T>` — **generics**: один интерфейс для разных сущностей.

### Как выглядит типичный метод (образец для объяснения)
```java
public Optional<Client> findById(int id) {
    String sql = SELECT + " WHERE id = ?";                       // параметризованный запрос
    try (Connection connection = DatabaseManager.getConnection();
         PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setInt(1, id);                                        // подставляем значение вместо «?»
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
        }
    } catch (SQLException e) {
        throw new DatabaseException("Ошибка выполнения SQL при поиске клиента", e);
    }
}
```
Что тут важно сказать:
- **`try-with-resources`** — `Connection`, `PreparedStatement`, `ResultSet` закрываются автоматически, даже при ошибке (не будет утечки соединений).
- **`PreparedStatement` с `?`** — значения подставляются отдельно от текста запроса.
- **`ResultSet`** — «курсор» по результату. `rs.next()` переходит к следующей строке, `mapRow(rs)` превращает строку БД в Java-объект.
- **`Optional`** — «может быть, а может не быть». Репозиторий не бросает ошибку «не найдено» — он возвращает пустой `Optional`, а сервис решает, что делать (бросить `EntityNotFoundException`).
- **`SQLException` оборачивается в `DatabaseException`** — верхние слои не знают про JDBC.

### `BookingRepository` — особенности
- **`SELECT_JOINED`** — базовый запрос с `JOIN clients` и `JOIN halls`: сразу получаем имя клиента и название зала.
- Внутренний функциональный интерфейс `ParamSetter` + метод `query(sql, лямбда, сообщение)` — чтобы не копировать в каждом методе один и тот же код (открыть соединение → подставить параметры → собрать список). Например:
  ```java
  return query(SELECT_JOINED + " WHERE LOWER(b.title) LIKE LOWER(?) ORDER BY b.start_time",
          ps -> ps.setString(1, "%" + text + "%"), "Ошибка ...");
  ```
- **Поиск** реализован SQL-запросами с `LIKE` (без учёта регистра): по названию, описанию, имени клиента; по дате — диапазон `>= начало дня AND < начало следующего дня`.
- `countByStatus()` — SQL-агрегация `GROUP BY status`, результат в `EnumMap`.
- `findByHallId()` — все брони зала (нужны для проверки пересечений времени).
- `save()` использует `prepareStatement(sql, new String[]{"id"})`, чтобы получить `id`, назначенный базой данных, и записать его в объект.

### `DatabaseViewRepository`
Читает **любую** из трёх таблиц через `ResultSetMetaData` (имена и количество колонок узнаём динамически). Имя таблицы в `SELECT * FROM <таблица>` подставляется строкой, потому что **имя таблицы нельзя передать параметром `?`** — поэтому имя берётся из **белого списка** `List.of("clients","halls","bookings")`, пользовательский ввод туда не попадает.

---

## 7. Пакет `service` (бизнес-логика)

Сервисы получают репозитории через конструктор (**внедрение зависимостей** вручную, в `Main`).

### `ClientService`
- `create/update`: ФИО ≥ 2 символов; телефон проверяется регулярным выражением (10–15 цифр, допускаются `+`, пробелы, скобки, дефисы — они убираются); email — по шаблону; **уникальность email**.
- `delete`: **нельзя удалить клиента, у которого есть бронирования**.

### `HallService`
Справочник залов: `getAll()`, `getById()`.

### `BookingService` — главный класс
Содержит бизнес-правила БП-1 … БП-11 (таблица в разделе 12).

**Метод `create` — образец:**
```java
validateTitle(title);
clientRepository.findById(clientId).orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
Hall hall = hallRepository.findById(hallId).orElseThrow(() -> new EntityNotFoundException("Зал", hallId));
validateDuration(hours);
validateWorkingHours(start, hours);
validateNotInPast(start);
validateNoOverlap(hallId, start, hours, 0);
BigDecimal price = calculatePrice(hall, start, hours);
...
```

**Проверка пересечения по времени** (самое «умное» правило):
```java
boolean overlaps = bookingRepository.findByHallId(hallId).stream()
    .filter(b -> b.getId() != excludeId)                          // не сравнивать бронь с самой собой при редактировании
    .filter(b -> b.getStatus() != BookingStatus.CANCELLED)        // отменённые зал не занимают
    .anyMatch(b -> start.isBefore(b.getEndTime()) && end.isAfter(b.getStartTime()));
```
Два интервала пересекаются, если **начало одного раньше конца другого и конец одного позже начала другого**. Если одна съёмка заканчивается в 12:00, а следующая начинается в 12:00 — это не пересечение.

**Смена статуса `changeStatus`:** проверяет, что статус действительно меняется, что переход разрешён (`canTransitionTo`), и что нельзя завершить ещё не начавшуюся съёмку.

**Поиск / фильтрация / сортировка:**
| Что | Как реализовано | Зачем так |
|---|---|---|
| Поиск (4 вида) | SQL `LIKE` в репозитории | показать работу с параметрами в `PreparedStatement` |
| Фильтры (4 вида) | Stream API: `findAll().stream().filter(...)` | требование КР: использовать Collections/Stream |
| Сортировки (4 вида) | `stream().sorted(comparator)` | то же; comparator лежит в enum `BookingSort` |

### Расчёт цены — интерфейс `PricingStrategy` (полиморфизм, паттерн «Стратегия»)
```java
public interface PricingStrategy {
    boolean isApplicable(LocalDateTime start, int hours);
    BigDecimal calculate(Hall hall, int hours);
    String getName();
}
```
Три реализации:

| Класс | Когда применяется | Формула |
|---|---|---|
| `WeekendPricing` | суббота, воскресенье | ставка × часы × **1.2** |
| `LongSessionPricing` | съёмка от 4 часов (в будни) | ставка × часы × **0.9** |
| `StandardPricing` | всегда (запасной) | ставка × часы |

В `Main` они собираются в список в **порядке приоритета**: `List.of(new WeekendPricing(), new LongSessionPricing(), new StandardPricing())`.
`BookingService.calculatePrice` берёт **первую подходящую**:
```java
PricingStrategy strategy = pricingStrategies.stream()
    .filter(s -> s.isApplicable(start, hours))
    .findFirst()
    .orElseThrow(...);
return strategy.calculate(hall, hours);
```
**Полиморфизм:** сервис вызывает `strategy.calculate(...)`, не зная, какой именно класс скрывается за переменной. Хотите новый тариф (ночной, скидка постоянным клиентам) — добавляете класс, **не меняя** `BookingService`.

### `StatisticsService`
14 показателей. Часть считает SQL (`COUNT`, `GROUP BY` в `countByStatus`), часть — Stream API (`filter → map → reduce`, `groupingBy → counting → max`). Результат — `LinkedHashMap<String, String>` (сохраняет порядок вставки, поэтому строки выводятся в нужном порядке).

### `ExportService`
Берёт данные из репозиториев и передаёт в `ExcelExporter` / `CsvExporter`, формирует имя файла с датой-временем (`photostudio_20260920_143015.xlsx`) в папке `export/`.

### `DatabaseViewService`
Прослойка, чтобы меню не обращалось к репозиторию напрямую (соблюдение слоёв).

---

## 8. Пакет `exception`

```
RuntimeException
 └── AppException                      — общий предок всех наших ошибок
      ├── BusinessException            — нарушено бизнес-правило
      ├── EntityNotFoundException      — запись с таким ID не найдена
      ├── DatabaseException            — ошибка подключения / SQL (обёртка над SQLException)
      ├── InvalidInputException        — неверный ввод (не число, неверная дата, номер не из списка)
      └── ExportException              — ошибка записи Excel/CSV
```
- Все они **непроверяемые** (`RuntimeException`), поэтому не нужно писать `throws` во всех методах.
- Общий предок `AppException` позволяет в одном месте (`Menu.run`) сделать `catch (AppException e)` и показать понятное сообщение.
- Порядок в `Menu.run()`: сначала `catch (AppException)`, потом `catch (RuntimeException)` — на случай непредвиденной ошибки, чтобы программа не упала.

**Какие ошибки из требований КР обработаны и где:**

| Требование КР | Класс исключения | Кто бросает | Сообщение пользователю |
|---|---|---|---|
| Текст вместо числа | `InvalidInputException` | `ConsoleInput.readInt` | «ID должен быть целым числом.» |
| Нет записи с ID | `EntityNotFoundException` | сервисы (`orElseThrow`) | «Клиент с ID 999 не найден(а).» |
| Нарушение бизнес-правила | `BusinessException` | сервисы | «Зал уже занят в это время…» |
| Ошибка подключения к БД | `DatabaseException` | `DatabaseManager` | при старте: сообщение + подсказка, программа корректно завершается |
| Ошибка SQL | `DatabaseException` | репозитории | «Ошибка выполнения SQL при …: <текст от PostgreSQL>» |

---

## 9. Пакет `ui` (консольные меню)

### `Menu` (абстрактный класс, паттерн «Шаблонный метод»)
Общий алгоритм в методе `run()`:
```
пока true:
    напечатать меню
    прочитать номер (ошибка ввода → сообщение → снова)
    0 → выйти из этого меню
    вне диапазона → сообщение → снова
    handle(номер)   ← это делает наследник
    ловим AppException → «Ошибка: ...» → снова меню
```
Наследники реализуют только: `getTitle()` (заголовок), `getItems()` (пункты), `handle(int)` (что делать). Поэтому программа **не завершается** после одного действия — она возвращается в меню.

Метод `printEntities(...)` принимает `List<? extends BaseEntity>` и для каждого элемента вызывает `describe()` — **полиморфизм**: клиент, зал и бронь печатаются по-разному одним и тем же кодом.

### Классы-меню
| Класс | Пункты |
|---|---|
| `MainMenu` | главное меню; запускает подменю, показывает статистику и таблицы БД |
| `ClientMenu` | добавить, все, по ID, изменить, удалить |
| `BookingMenu` | создать, все, по ID, изменить, сменить статус, удалить |
| `SearchMenu` | 4 вида поиска |
| `FilterMenu` | 4 фильтра |
| `SortMenu` | пункты строятся из `BookingSort.values()` |
| `ExportMenu` | Excel, CSV |

При редактировании (`edit()`) в квадратных скобках показывается текущее значение; **Enter — оставить без изменений** (методы `readLine(prompt, current)`, `readInt(..., current)`, `readDateTime(..., current)`).

### `ConsoleInput`
Единственное место, где используется `Scanner`. Читает строку/число/дату/enum; при неверном формате бросает `InvalidInputException`. Формат даты — **строгий** (`ResolverStyle.STRICT`): 31.02.2026 будет отклонена.
Метод `readEnum` — **generic** с ограничением `<E extends Enum<E> & Titled>`: работает с любым enum, у которого есть `getTitle()`.

### `TablePrinter`
Рисует таблицу БД в консоли, вычисляя ширину каждой колонки.

---

## 10. Пакет `util` и класс `Main`

### `DatabaseManager`
Единственное место, где создаётся `Connection` (`DriverManager.getConnection(url, user, password)`). Настройки читает из `db.properties` (рядом с программой или из `resources`), переменные окружения `DB_URL/DB_USER/DB_PASSWORD` имеют приоритет. Метод `checkConnection()` вызывается при старте: если БД недоступна — выводится понятное сообщение, а не «простыня» стек-трейса.

### `ExcelExporter` (Apache POI)
Создаёт `.xlsx` с тремя листами: **Клиенты**, **Залы**, **Бронирования**. Заголовки — жирные, белым по синему; даты — настоящие даты Excel (формат `дд.мм.гггг чч:мм`), деньги — числа с форматом `#,##0.00`; закреплена первая строка, включён автофильтр.

### `CsvExporter`
Дополнительный экспорт броней в CSV: разделитель `;`, кодировка UTF-8 с BOM (чтобы Excel правильно показал русские буквы), значения с `;` и кавычками экранируются.

### `Formats`
Общие форматы даты/времени и метод `money(...)` (`3600.00 ₽`).

### `Main`
Только «сборка» приложения (**композиция**): создаёт репозитории → сервисы → меню и запускает `mainMenu.run()`. Ни SQL, ни бизнес-логики, ни `Scanner` здесь нет.
```java
ClientRepository clientRepository = new ClientRepository();
...
BookingService bookingService = new BookingService(bookingRepository, clientRepository, hallRepository, pricingStrategies);
...
mainMenu.run();
```

---

## 11. Где в коде каждая «ключевая тема» защиты

| Тема из задания КР | Где смотреть | Что сказать |
|---|---|---|
| Назначение основных классов | раздел 2 и таблицы выше | 4 слоя, у каждого своя ответственность |
| **Инкапсуляция** | `Client`, `Booking`, `Hall` | поля `private`, доступ через геттеры/сеттеры; изменить статус можно только через `BookingService.changeStatus`, где есть проверки |
| **Интерфейсы** | `CrudRepository`, `ReadRepository`, `PricingStrategy`, `Titled` | интерфейс — контракт «что умеет», реализация — «как» |
| **Enum** | `BookingStatus`, `SessionType`, `BookingSort` | ограниченный набор допустимых значений + поведение внутри enum |
| **Полиморфизм** | `BaseEntity.describe()`; `PricingStrategy`; `Menu.handle()` | один вызов — разное поведение в зависимости от реального класса |
| **Коллекции** | `List`, `Optional`, `EnumMap`, `LinkedHashMap`, Stream API | `List` — списки записей; `EnumMap` — счётчики по статусам; `LinkedHashMap` — статистика в порядке добавления |
| **Обработка исключений** | пакет `exception`, `Menu.run()` | свои исключения, один общий обработчик, программа не падает |
| **JDBC** | `repository/*`, `DatabaseManager` | `Connection` → `PreparedStatement` → `ResultSet`, всё в `try-with-resources` |
| Отличие **Statement** от **PreparedStatement** | вопрос 8 ниже | защита от SQL-инъекций, параметры, кеширование плана |
| Связи между таблицами | `sql/photostudio.sql`, `docs/er_diagram.png` | `bookings.client_id → clients.id`, `bookings.hall_id → halls.id` |
| Бизнес-правила | раздел 12 | 11 правил в Java-коде |
| Создание / изменение / удаление | `BookingMenu`, `BookingService`, `BookingRepository` | INSERT / UPDATE / DELETE через `PreparedStatement` |
| Поиск, фильтрация, сортировка | `SearchMenu`, `FilterMenu`, `SortMenu` | поиск — SQL `LIKE`; фильтры и сортировки — Stream API |
| Экспорт данных | `ExcelExporter`, `ExportService` | Apache POI, три листа |

---

## 12. Бизнес-правила

Все правила реализованы **в Java-коде** (сервисы и enum), а не только в меню.

| № | Правило | Где | Сообщение при нарушении |
|---|---|---|---|
| БП-1 | Название съёмки обязательно, до 100 символов | `BookingService.validateTitle` | «Название съёмки обязательно.» |
| БП-2 | Клиент должен существовать | `BookingService.create` | «Клиент с ID 99 не найден(а).» |
| БП-3 | Зал должен существовать | `BookingService.create/update` | «Зал с ID 99 не найден(а).» |
| БП-4 | Длительность 1–8 часов | `validateDuration` | «Длительность съёмки должна быть от 1 до 8 часов.» |
| БП-5 | Съёмка в рабочее время студии 09:00–21:00, в пределах одного дня | `validateWorkingHours` | «Студия работает с 09:00 до 21:00…» |
| БП-6 | Нельзя бронировать на прошедшее время | `validateNotInPast` | «Нельзя создать бронь на прошедшее время.» |
| БП-7 | Зал не может быть занят двумя бронями одновременно (отменённые не считаются) | `validateNoOverlap` | «Зал уже занят в это время…» |
| БП-8 | Только допустимые переходы статусов | `BookingStatus.canTransitionTo`, `changeStatus` | «Недопустимый переход статуса: «Создана» → «Завершена».» |
| БП-9 | Нельзя завершить съёмку, которая ещё не началась | `changeStatus` | «Нельзя завершить фотосессию, которая ещё не началась.» |
| БП-10 | Завершённые/отменённые брони нельзя изменять; завершённые нельзя удалять | `update`, `delete` | «Нельзя изменять бронь в статусе «Завершена».» |
| БП-11 | Стоимость считается автоматически по тарифу (пользователь её не вводит) | `calculatePrice` + `PricingStrategy` | — |
| Доп. | ФИО ≥ 2 символов, корректные телефон и email, **email уникален** | `ClientService.validate/create` | «Клиент с email … уже существует.» |
| Доп. | Нельзя удалить клиента с бронированиями | `ClientService.delete` | «Нельзя удалить клиента, у которого есть бронирования.» |

---

## 13. Вопросы преподавателя и готовые ответы

**1. Почему вы не написали всё в `Main`?**
Так код невозможно поддерживать и расширять. Мы разделили ответственность: `ui` — общение с пользователем, `service` — правила, `repository` — SQL, `model` — данные. `Main` только собирает части. Такая структура позволит в КР 2–4 заменить консоль на JavaFX, Spring REST или Android, не переписывая сервисы и репозитории.

**2. Что такое инкапсуляция и где она у вас?**
Скрытие внутреннего состояния объекта: поля `private`, доступ через методы. Например, в `Booking` нельзя «просто так» поставить статус — сервис проверяет допустимость перехода в `changeStatus`. Также `Hall` вообще неизменяем (все поля `final`).

**3. Зачем нужны интерфейсы? Какие у вас?**
Интерфейс описывает «что можно делать», не привязываясь к реализации. `CrudRepository<T>` — набор операций для любых сущностей; `PricingStrategy` — расчёт цены, у которого три реализации; `Titled` — «у enum есть русское название», что позволило написать один универсальный `readEnum`.

**4. Где у вас полиморфизм?**
(1) `BaseEntity.describe()` — `printEntities` вызывает его у клиентов, залов и броней, и каждый печатается по-своему. (2) `PricingStrategy` — сервис вызывает `calculate()`, не зная, какой именно тариф. (3) `Menu.handle()` — общий цикл `run()` вызывает `handle()`, а действие определяет конкретное меню.

**5. Зачем нужен enum? Почему не строки?**
Enum ограничивает набор значений на этапе компиляции: невозможно опечататься («CONFRIMED»). К тому же в нём можно держать логику — у `BookingStatus` есть `canTransitionTo()` с правилами переходов.

**6. Какие коллекции используете и зачем?**
`List` — результаты запросов; `Optional` — «найдено / не найдено»; `EnumMap<BookingStatus,Integer>` — количество по статусам (быстрая структура для ключей-enum); `LinkedHashMap` — статистика в порядке добавления; Stream API — фильтрация, сортировка, группировка, суммирование.

**7. Как работает JDBC? Опишите порядок.**
Получаем `Connection` через `DriverManager` → готовим `PreparedStatement` с SQL и параметрами `?` → подставляем значения `setInt/setString/...` → `executeQuery()` (SELECT) или `executeUpdate()` (INSERT/UPDATE/DELETE) → для SELECT перебираем `ResultSet` через `rs.next()` → ресурсы закрываются автоматически благодаря `try-with-resources`.

**8. Чем `Statement` отличается от `PreparedStatement`?**
`Statement` выполняет готовую строку SQL, в которую значения приходится «склеивать» конкатенацией — это открывает путь для **SQL-инъекций** (например, ввод `' OR '1'='1`). `PreparedStatement` принимает SQL с плейсхолдерами `?`, значения передаются отдельно и всегда трактуются как данные, а не как код. Кроме того, база может кешировать план запроса, и не нужно вручную экранировать кавычки и форматировать даты/числа. В нашем проекте `Statement` вообще не используется.

**9. Что такое SQL-инъекция? Есть ли у вас уязвимости?**
Когда пользовательский ввод становится частью SQL-команды. У нас везде `PreparedStatement`. Единственное место, где в SQL подставляется строка — имя таблицы в `DatabaseViewRepository`, но она берётся из закреплённого белого списка, а не из ввода.

**10. Зачем `try-with-resources`?**
`Connection`, `PreparedStatement`, `ResultSet` нужно закрывать. Конструкция закрывает их автоматически в правильном порядке, даже если в блоке возникло исключение — нет утечек соединений.

**11. Как связаны таблицы?**
`bookings.client_id → clients.id` и `bookings.hall_id → halls.id` (внешние ключи). Связь «один ко многим»: у одного клиента (зала) много броней. Внешний ключ не даст создать бронь для несуществующего клиента.

**12. Почему бизнес-правила в сервисе, а не в меню или SQL?**
В меню их проверять нельзя: при переходе к JavaFX/REST/Android их пришлось бы дублировать. Сервис — единое место, откуда правила действуют для любого интерфейса. Ограничения БД (`CHECK`, `FK`, `UNIQUE`) — дополнительная страховка.

**13. Как проверяется пересечение броней по времени?**
Берём все брони зала (`findByHallId`), отбрасываем отменённые и ту, что редактируем, и проверяем условие `новое.начало < чужое.конец && новое.конец > чужое.начало`.

**14. Как считается цена?**
Паттерн «Стратегия»: три класса-тарифа реализуют `PricingStrategy`. Сервис берёт первый подходящий: выходной (+20%), длительная съёмка от 4 часов (−10%), иначе обычный. Пример: Green Garden (2000 ₽/час), воскресенье, 3 часа → 2000 × 3 × 1.2 = 7200 ₽.

**15. Как обрабатываются ошибки?**
Собственная иерархия: `AppException` → `BusinessException`, `EntityNotFoundException`, `DatabaseException`, `InvalidInputException`, `ExportException`. Сервисы и репозитории бросают их, а единый `catch (AppException)` в `Menu.run()` печатает «Ошибка: …» и возвращает пользователя в меню. Ошибку подключения к БД мы проверяем при старте.

**16. Почему исключения `RuntimeException`, а не проверяемые?**
Чтобы не загромождать сигнатуры методов `throws` через все слои. Для ошибок, от которых пользователь может оправиться (неверный ввод, нарушение правила), достаточно одного общего обработчика в меню.

**17. Как работает экспорт в Excel?**
Apache POI: создаём `XSSFWorkbook`, для каждой сущности — лист, строка заголовков со стилем, затем строки данных; тип значения определяет тип ячейки (число, деньги, дата, текст). Файл пишем в `export/`.

**18. Как устроены поиск, фильтрация и сортировка?**
Поиск — SQL `LIKE` в репозитории (параметр `%текст%`, без учёта регистра). Фильтры — Stream API: `findAll().stream().filter(...)`. Сортировка — `stream().sorted(comparator)`, компараторы хранятся в enum `BookingSort`.

**19. Что будет, если ввести буквы вместо числа?**
`ConsoleInput.readInt` ловит `NumberFormatException`, бросает `InvalidInputException("ID должен быть целым числом.")`; `Menu.run()` печатает сообщение и показывает меню снова. Программа не падает.

**20. Как добавить новый тип съёмки / новый тариф?**
Новый тип — значение в enum `SessionType` (+ значение в `CHECK` таблицы). Новый тариф — новый класс, реализующий `PricingStrategy`, и добавление его в список в `Main`. Остальной код менять не нужно.

**21. Что делает `Optional` и `orElseThrow`?**
`Optional<T>` — контейнер «есть значение или нет». `orElseThrow(() -> new EntityNotFoundException(...))` — вернуть значение, а если его нет — бросить исключение. Так мы избегаем `null` и `NullPointerException`.

**22. Что такое `Comparator` и как работает `sorted`?**
`Comparator<Booking>` — правило сравнения двух объектов. `Comparator.comparing(Booking::getPrice).reversed()` — сравнить по цене и развернуть порядок. `stream().sorted(comparator)` возвращает новый отсортированный поток; исходный список не меняется.

**23. Зачем `BigDecimal` для денег?**
`double` даёт погрешности округления (0.1 + 0.2 ≠ 0.3). `BigDecimal` хранит десятичное число точно; в БД — `NUMERIC(10,2)`.

**24. Зачем в `Booking` поля `clientName` и `hallName`, если в БД их нет?**
Для удобного вывода. Репозиторий заполняет их через `JOIN` одним запросом; иначе для каждой брони пришлось бы делать два дополнительных запроса за именем клиента и названием зала.

---

## 14. Как поделить рассказ между 4 участниками

| Участник | Тема | Файлы |
|---|---|---|
| **1** | База данных и модель: таблицы, связи, ограничения, ER-диаграмма, enum, наследование `BaseEntity`, инкапсуляция | `sql/photostudio.sql`, `docs/er_diagram.png`, `model/*` |
| **2** | JDBC и репозитории: `DatabaseManager`, `PreparedStatement`, `ResultSet`, `try-with-resources`, Statement vs PreparedStatement, `Optional`, поиск LIKE | `util/DatabaseManager`, `repository/*` |
| **3** | Бизнес-логика: правила БП-1…БП-11, переходы статусов, пересечение по времени, стратегии цены, фильтры/сортировки/статистика на Stream API, исключения | `service/*`, `exception/*` |
| **4** | Консольный интерфейс и экспорт: `Menu` (шаблонный метод), `ConsoleInput`, обработка ввода, `Main` (сборка слоёв), Excel/CSV, демонстрация запуска | `ui/*`, `Main`, `util/ExcelExporter`, `CsvExporter` |

Но помните: по условию КР **должны понимать проект все** — прочитайте разделы 2, 3, 11 и 13 целиком.
