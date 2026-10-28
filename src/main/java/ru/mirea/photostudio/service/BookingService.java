package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.exception.ValidationException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.BookingSortField;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.repository.PaymentRepository;
import ru.mirea.photostudio.repository.PhotographerRepository;
import ru.mirea.photostudio.service.pricing.PricingContext;
import ru.mirea.photostudio.service.pricing.PricingStrategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Бизнес-логика бронирований. Основные бизнес-правила (каждое проверяется здесь, в Java-коде):
 * <ol>
 *   <li>клиент должен существовать;</li>
 *   <li>фотограф должен существовать и работать (active);</li>
 *   <li>зал должен существовать;</li>
 *   <li>тип съёмки, время начала и окончания обязательны;</li>
 *   <li>окончание позже начала; съёмка укладывается в одни сутки;</li>
 *   <li>новое время начала не может быть в прошлом;</li>
 *   <li>студия работает с 09:00 до 21:00;</li>
 *   <li>длительность от 1 до 8 часов, кратна 30 минутам;</li>
 *   <li>число участников от 1 до вместимости зала;</li>
 *   <li>фотограф не может иметь два пересекающихся активных бронирования;</li>
 *   <li>зал не может иметь два пересекающихся активных бронирования;</li>
 *   <li>допустимые переходы статусов: CREATED→CONFIRMED|CANCELLED, CONFIRMED→COMPLETED|CANCELLED;</li>
 *   <li>подтвердить можно только будущее бронирование, завершить — только начавшееся;</li>
 *   <li>завершённое и отменённое бронирование нельзя изменять;</li>
 *   <li>нельзя удалить подтверждённое/завершённое бронирование и бронирование с платежами.</li>
 * </ol>
 */
public class BookingService {
    public static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    public static final LocalTime CLOSE_TIME = LocalTime.of(21, 0);
    private static final long MIN_MINUTES = 60;
    private static final long MAX_MINUTES = 8 * 60;
    private static final long STEP_MINUTES = 30;

    private final BookingRepository bookings;
    private final ClientRepository clients;
    private final PhotographerRepository photographers;
    private final HallRepository halls;
    private final PaymentRepository payments;
    private final PricingStrategy pricing;

    public BookingService(BookingRepository bookings, ClientRepository clients, PhotographerRepository photographers,
                          HallRepository halls, PaymentRepository payments, PricingStrategy pricing) {
        this.bookings = bookings;
        this.clients = clients;
        this.photographers = photographers;
        this.halls = halls;
        this.payments = payments;
        this.pricing = pricing;
    }

    // ---------- CRUD ----------

    public Booking create(Booking booking) {
        validateAndPrice(booking, true, 0);
        booking.setStatus(BookingStatus.CREATED);
        booking.setCreatedAt(LocalDateTime.now());
        return bookings.create(booking);
    }

    public List<Booking> getAll() {
        return bookings.findAll();
    }

    public Booking getById(long id) {
        return bookings.findById(id).orElseThrow(() -> new EntityNotFoundException("Бронирование", id));
    }

    /** Изменение параметров брони. Статус и клиент не меняются (статус — через changeStatus). */
    public Booking update(Booking booking) {
        Booking existing = getById(booking.getId());
        if (existing.getStatus().isFinal()) {
            throw new BusinessException("Нельзя изменять бронирование со статусом «"
                    + existing.getStatus().getTitle() + "»");
        }
        booking.setClientId(existing.getClientId());
        booking.setStatus(existing.getStatus());
        booking.setCreatedAt(existing.getCreatedAt());
        boolean startChanged = !existing.getStartTime().equals(booking.getStartTime());
        validateAndPrice(booking, startChanged, booking.getId());
        bookings.update(booking);
        return booking;
    }

    public void delete(long id) {
        Booking booking = getById(id);
        if (booking.getStatus() == BookingStatus.CONFIRMED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Нельзя удалить подтверждённое или завершённое бронирование");
        }
        if (payments.existsByBookingId(id)) {
            throw new BusinessException("Нельзя удалить бронирование, по которому есть платежи");
        }
        bookings.deleteById(id);
    }

    public Booking changeStatus(long id, BookingStatus next) {
        Booking booking = getById(id);
        BookingStatus current = booking.getStatus();
        if (next == null) {
            throw new ValidationException("Не указан новый статус");
        }
        if (current == next) {
            throw new BusinessException("Бронирование уже имеет статус «" + next.getTitle() + "»");
        }
        if (!current.canTransitionTo(next)) {
            throw new BusinessException("Недопустимый переход статуса: «" + current.getTitle()
                    + "» → «" + next.getTitle() + "»");
        }
        LocalDateTime now = LocalDateTime.now();
        if (next == BookingStatus.CONFIRMED && booking.getStartTime().isBefore(now)) {
            throw new BusinessException("Нельзя подтвердить бронирование, время которого уже прошло");
        }
        if (next == BookingStatus.COMPLETED && booking.getStartTime().isAfter(now)) {
            throw new BusinessException("Нельзя завершить бронирование до начала съёмки");
        }
        booking.setStatus(next);
        bookings.update(booking);
        return booking;
    }

    // ---------- Поиск ----------

    public List<Booking> searchByClientName(String query) {
        String q = ClientService.normalizeQuery(query);
        Set<Long> ids = clients.findAll().stream()
                .filter(c -> c.getFullName().toLowerCase(Locale.ROOT).contains(q))
                .map(Client::getId)
                .collect(Collectors.toSet());
        return bookings.findAll().stream().filter(b -> ids.contains(b.getClientId())).toList();
    }

    public List<Booking> searchByPhotographerName(String query) {
        String q = ClientService.normalizeQuery(query);
        Set<Long> ids = photographers.findAll().stream()
                .filter(p -> p.getFullName().toLowerCase(Locale.ROOT).contains(q))
                .map(Photographer::getId)
                .collect(Collectors.toSet());
        return bookings.findAll().stream().filter(b -> ids.contains(b.getPhotographerId())).toList();
    }

    public List<Booking> searchByDate(LocalDate date) {
        return bookings.findByDate(date);
    }

    // ---------- Фильтрация (Stream API) ----------

    public List<Booking> filterByStatus(BookingStatus status) {
        return bookings.findAll().stream().filter(b -> b.getStatus() == status).toList();
    }

    public List<Booking> filterBySessionType(SessionType type) {
        return bookings.findAll().stream().filter(b -> b.getSessionType() == type).toList();
    }

    public List<Booking> filterByDateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("Начало диапазона дат не может быть позже его конца");
        }
        return bookings.findAll().stream()
                .filter(b -> {
                    LocalDate date = b.getStartTime().toLocalDate();
                    return !date.isBefore(from) && !date.isAfter(to);
                })
                .toList();
    }

    // ---------- Сортировка (Stream API + Comparator) ----------

    public List<Booking> sortByStartTime(boolean ascending) {
        return sorted(Comparator.comparing(Booking::getStartTime), ascending);
    }

    public List<Booking> sortByPrice(boolean ascending) {
        return sorted(Comparator.comparing(Booking::getTotalPrice), ascending);
    }

    public List<Booking> sortByCreatedAt(boolean ascending) {
        return sorted(Comparator.comparing(Booking::getCreatedAt), ascending);
    }

    private List<Booking> sorted(Comparator<Booking> comparator, boolean ascending) {
        return bookings.findAll().stream()
                .sorted(ascending ? comparator : comparator.reversed())
                .toList();
    }

    // ---------- Проверки и расчёт ----------

    private void validateAndPrice(Booking b, boolean checkFuture, long excludeId) {
        Client client = clients.findById(b.getClientId()).orElseThrow(() ->
                new BusinessException("Нельзя создать бронирование для несуществующего клиента (ID "
                        + b.getClientId() + ")"));
        Photographer photographer = photographers.findById(b.getPhotographerId()).orElseThrow(() ->
                new BusinessException("Фотограф с ID " + b.getPhotographerId() + " не найден"));
        if (!photographer.isActive()) {
            throw new BusinessException("Фотограф «" + photographer.getFullName() + "» сейчас не работает");
        }
        Hall hall = halls.findById(b.getHallId()).orElseThrow(() ->
                new BusinessException("Зал с ID " + b.getHallId() + " не найден"));

        if (b.getSessionType() == null) {
            throw new ValidationException("Не указан тип съёмки");
        }
        LocalDateTime start = b.getStartTime();
        LocalDateTime end = b.getEndTime();
        if (start == null || end == null) {
            throw new ValidationException("Не указано время начала или окончания");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("Время окончания должно быть позже времени начала");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessException("Съёмка должна начинаться и заканчиваться в один день");
        }
        if (checkFuture && start.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Нельзя назначить съёмку на прошедшее время");
        }
        if (start.toLocalTime().isBefore(OPEN_TIME) || end.toLocalTime().isAfter(CLOSE_TIME)) {
            throw new BusinessException("Студия работает с " + OPEN_TIME + " до " + CLOSE_TIME);
        }
        long minutes = b.getDurationMinutes();
        if (minutes < MIN_MINUTES || minutes > MAX_MINUTES) {
            throw new BusinessException("Длительность съёмки должна быть от 1 до 8 часов");
        }
        if (minutes % STEP_MINUTES != 0) {
            throw new BusinessException("Длительность съёмки должна быть кратна 30 минутам");
        }
        if (b.getParticipants() < 1) {
            throw new BusinessException("Количество участников должно быть не меньше 1");
        }
        if (b.getParticipants() > hall.getCapacity()) {
            throw new BusinessException("Зал «" + hall.getName() + "» вмещает не более "
                    + hall.getCapacity() + " человек");
        }
        if (!bookings.findActiveOverlappingForPhotographer(photographer.getId(), start, end, excludeId).isEmpty()) {
            throw new BusinessException("Фотограф «" + photographer.getFullName() + "» занят в это время");
        }
        if (!bookings.findActiveOverlappingForHall(hall.getId(), start, end, excludeId).isEmpty()) {
            throw new BusinessException("Зал «" + hall.getName() + "» занят в это время");
        }
        if (b.getComment() != null && b.getComment().length() > 255) {
            throw new ValidationException("Комментарий не должен превышать 255 символов");
        }

        b.setTotalPrice(price(b, client, photographer, hall));
    }

    private BigDecimal price(Booking b, Client client, Photographer photographer, Hall hall) {
        if (b.getSessionType() == null || b.getStartTime() == null || b.getEndTime() == null) {
            throw new ValidationException("Для расчёта стоимости укажите тип съёмки, дату и время");
        }
        BigDecimal hours = BigDecimal.valueOf(b.getDurationMinutes()).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return pricing.calculate(new PricingContext(b.getSessionType(), hours,
                photographer.getHourlyRate(), hall.getHourlyPrice(), b.getStartTime(), client.isVip()));
    }

    // ---------- Методы для графического интерфейса (КР 2) ----------

    private record Lookup(Map<Long, String> clients, Map<Long, String> photographers,
                          Map<Long, String> halls, Map<Long, BigDecimal> paid) {
    }

    private Lookup lookup() {
        Map<Long, String> clientNames = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getFullName));
        Map<Long, String> photographerNames = photographers.findAll().stream()
                .collect(Collectors.toMap(Photographer::getId, Photographer::getFullName));
        Map<Long, String> hallNames = halls.findAll().stream()
                .collect(Collectors.toMap(Hall::getId, Hall::getName));
        Map<Long, BigDecimal> paid = payments.findAll().stream()
                .collect(Collectors.groupingBy(Payment::getBookingId,
                        Collectors.reducing(BigDecimal.ZERO, Payment::getAmount, BigDecimal::add)));
        return new Lookup(clientNames, photographerNames, hallNames, paid);
    }

    private BookingView view(Booking b, Lookup l) {
        return new BookingView(b,
                l.clients().getOrDefault(b.getClientId(), "ID " + b.getClientId()),
                l.photographers().getOrDefault(b.getPhotographerId(), "ID " + b.getPhotographerId()),
                l.halls().getOrDefault(b.getHallId(), "ID " + b.getHallId()),
                l.paid().getOrDefault(b.getId(), BigDecimal.ZERO));
    }

    public List<BookingView> toViews(List<Booking> list) {
        Lookup l = lookup();
        return list.stream().map(b -> view(b, l)).toList();
    }

    public BookingView getView(long id) {
        return view(getById(id), lookup());
    }

    public List<Booking> getByClientId(long clientId) {
        return bookings.findAll().stream().filter(b -> b.getClientId() == clientId).toList();
    }

    /** Предварительный расчёт стоимости для формы (без проверки занятости и времени). */
    public BigDecimal estimatePrice(Booking b) {
        Client client = clients.findById(b.getClientId())
                .orElseThrow(() -> new BusinessException("Клиент не выбран"));
        Photographer photographer = photographers.findById(b.getPhotographerId())
                .orElseThrow(() -> new BusinessException("Фотограф не выбран"));
        Hall hall = halls.findById(b.getHallId())
                .orElseThrow(() -> new BusinessException("Зал не выбран"));
        if (!b.getEndTime().isAfter(b.getStartTime())) {
            throw new BusinessException("Время окончания должно быть позже времени начала");
        }
        return price(b, client, photographer, hall);
    }

    /**
     * Единая выборка для таблицы: поиск по нескольким полям (клиент, фотограф, зал, комментарий,
     * тип съёмки, номер), фильтры по статусу, типу и периоду дат и сортировка.
     */
    public List<BookingView> query(BookingQuery q) {
        if (q.from() != null && q.to() != null && q.from().isAfter(q.to())) {
            throw new BusinessException("Дата «с» не может быть позже даты «по»");
        }
        String text = q.text() == null ? "" : q.text().trim().toLowerCase(Locale.ROOT);
        BookingSortField field = q.sortField() == null ? BookingSortField.START_TIME : q.sortField();
        Comparator<BookingView> comparator = switch (field) {
            case START_TIME -> Comparator.comparing((BookingView v) -> v.booking().getStartTime());
            case PRICE -> Comparator.comparing((BookingView v) -> v.booking().getTotalPrice());
            case CREATED_AT -> Comparator.comparing((BookingView v) -> v.booking().getCreatedAt());
            case CLIENT_NAME -> Comparator.comparing((BookingView v) -> v.clientName().toLowerCase(Locale.ROOT));
        };
        if (!q.ascending()) {
            comparator = comparator.reversed();
        }
        Lookup l = lookup();
        return bookings.findAll().stream()
                .map(b -> view(b, l))
                .filter(v -> matchesText(v, text))
                .filter(v -> q.status() == null || v.booking().getStatus() == q.status())
                .filter(v -> q.sessionType() == null || v.booking().getSessionType() == q.sessionType())
                .filter(v -> q.from() == null || !v.booking().getStartTime().toLocalDate().isBefore(q.from()))
                .filter(v -> q.to() == null || !v.booking().getStartTime().toLocalDate().isAfter(q.to()))
                .sorted(comparator)
                .toList();
    }

    private boolean matchesText(BookingView v, String text) {
        if (text.isEmpty()) {
            return true;
        }
        Booking b = v.booking();
        return String.valueOf(b.getId()).equals(text)
                || v.clientName().toLowerCase(Locale.ROOT).contains(text)
                || v.photographerName().toLowerCase(Locale.ROOT).contains(text)
                || v.hallName().toLowerCase(Locale.ROOT).contains(text)
                || b.getSessionType().getTitle().toLowerCase(Locale.ROOT).contains(text)
                || (b.getComment() != null && b.getComment().toLowerCase(Locale.ROOT).contains(text));
    }
}
