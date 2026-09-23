package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingSort;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.service.pricing.PricingStrategy;
import ru.mirea.photostudio.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Главный сервис: здесь живут ВСЕ бизнес-правила бронирования.
 * Консольное меню только вызывает методы этого класса и выводит результат.
 *
 * Бизнес-правила:
 *  БП-1  Название съёмки обязательно (не пустое, до 100 символов).
 *  БП-2  Клиент должен существовать.
 *  БП-3  Зал должен существовать.
 *  БП-4  Длительность съёмки — от 1 до 8 часов.
 *  БП-5  Съёмка должна проходить в рабочее время студии: 09:00–21:00 (в пределах одного дня).
 *  БП-6  Нельзя создать бронь на прошедшее время.
 *  БП-7  Зал не может быть забронирован на пересекающееся время (отменённые брони не считаются).
 *  БП-8  Разрешены только допустимые переходы статусов (см. BookingStatus.canTransitionTo).
 *  БП-9  Нельзя завершить съёмку, которая ещё не началась.
 *  БП-10 Завершённые и отменённые брони нельзя изменять; завершённые нельзя удалять.
 *  БП-11 Стоимость рассчитывается автоматически по тарифу (PricingStrategy), вручную её ввести нельзя.
 */
public class BookingService {

    public static final LocalTime STUDIO_OPEN = LocalTime.of(9, 0);
    public static final LocalTime STUDIO_CLOSE = LocalTime.of(21, 0);
    public static final int MIN_HOURS = 1;
    public static final int MAX_HOURS = 8;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final HallRepository hallRepository;
    private final List<PricingStrategy> pricingStrategies;

    public BookingService(BookingRepository bookingRepository, ClientRepository clientRepository,
                          HallRepository hallRepository, List<PricingStrategy> pricingStrategies) {
        this.bookingRepository = bookingRepository;
        this.clientRepository = clientRepository;
        this.hallRepository = hallRepository;
        this.pricingStrategies = pricingStrategies;
    }

    // ============================================================== CRUD

    public Booking create(int clientId, int hallId, String title, String description,
                          SessionType type, LocalDateTime start, int hours) {
        validateTitle(title);
        validateDescription(description);
        clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new EntityNotFoundException("Зал", hallId));
        validateDuration(hours);
        validateWorkingHours(start, hours);
        validateNotInPast(start);
        validateNoOverlap(hallId, start, hours, 0);

        BigDecimal price = calculatePrice(hall, start, hours);
        Booking booking = new Booking(clientId, hallId, title.trim(), normalize(description),
                type, start, hours, price);
        Booking saved = bookingRepository.save(booking);
        return getById(saved.getId()); // перечитываем, чтобы подтянуть имя клиента и зала
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    public Booking getById(int id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Бронирование", id));
    }

    public Booking update(int id, int hallId, String title, String description,
                          SessionType type, LocalDateTime start, int hours) {
        Booking booking = getById(id);
        if (booking.getStatus().isFinal()) {
            throw new BusinessException("Нельзя изменять бронь в статусе «"
                    + booking.getStatus().getTitle() + "».");
        }
        validateTitle(title);
        validateDescription(description);
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new EntityNotFoundException("Зал", hallId));
        validateDuration(hours);
        validateWorkingHours(start, hours);
        if (!start.equals(booking.getStartTime())) {
            validateNotInPast(start); // прошлую дату проверяем только если время изменили
        }
        validateNoOverlap(hallId, start, hours, id);

        booking.setHallId(hallId);
        booking.setTitle(title.trim());
        booking.setDescription(normalize(description));
        booking.setSessionType(type);
        booking.setStartTime(start);
        booking.setDurationHours(hours);
        booking.setPrice(calculatePrice(hall, start, hours));
        bookingRepository.update(booking);
        return getById(id);
    }

    public Booking changeStatus(int id, BookingStatus newStatus) {
        Booking booking = getById(id);
        BookingStatus current = booking.getStatus();
        if (current == newStatus) {
            throw new BusinessException("Бронь уже имеет статус «" + current.getTitle() + "».");
        }
        if (!current.canTransitionTo(newStatus)) {
            throw new BusinessException("Недопустимый переход статуса: «" + current.getTitle()
                    + "» → «" + newStatus.getTitle() + "».");
        }
        if (newStatus == BookingStatus.COMPLETED && booking.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Нельзя завершить фотосессию, которая ещё не началась.");
        }
        booking.setStatus(newStatus);
        bookingRepository.update(booking);
        return booking;
    }

    public void delete(int id) {
        Booking booking = getById(id);
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Нельзя удалить завершённую бронь (она нужна для отчётности).");
        }
        bookingRepository.deleteById(id);
    }

    // ============================================================== поиск (SQL LIKE)

    public List<Booking> searchByTitle(String text) {
        return bookingRepository.searchByTitle(requireQuery(text));
    }

    public List<Booking> searchByDescription(String text) {
        return bookingRepository.searchByDescription(requireQuery(text));
    }

    public List<Booking> searchByClientName(String text) {
        return bookingRepository.searchByClientName(requireQuery(text));
    }

    public List<Booking> searchByDate(LocalDate date) {
        return bookingRepository.searchByDate(date);
    }

    // ============================================================== фильтрация (Stream API)

    public List<Booking> filterByStatus(BookingStatus status) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getStatus() == status)
                .toList();
    }

    public List<Booking> filterBySessionType(SessionType type) {
        return bookingRepository.findAll().stream()
                .filter(b -> b.getSessionType() == type)
                .toList();
    }

    public List<Booking> filterByDateRange(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BusinessException("Конечная дата не может быть раньше начальной.");
        }
        return bookingRepository.findAll().stream()
                .filter(b -> {
                    LocalDate day = b.getStartTime().toLocalDate();
                    return !day.isBefore(from) && !day.isAfter(to);
                })
                .sorted(BookingSort.BY_START_TIME.getComparator())
                .toList();
    }

    public List<Booking> filterByHall(int hallId) {
        hallRepository.findById(hallId).orElseThrow(() -> new EntityNotFoundException("Зал", hallId));
        return bookingRepository.findAll().stream()
                .filter(b -> b.getHallId() == hallId)
                .toList();
    }

    // ============================================================== сортировка (Comparator)

    public List<Booking> sort(BookingSort sort) {
        return bookingRepository.findAll().stream()
                .sorted(sort.getComparator())
                .toList();
    }

    // ============================================================== правила и проверки

    /** БП-11: перебираем стратегии, берём первую подходящую (последняя — стандартная — подходит всегда). */
    private BigDecimal calculatePrice(Hall hall, LocalDateTime start, int hours) {
        PricingStrategy strategy = pricingStrategies.stream()
                .filter(s -> s.isApplicable(start, hours))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Не найден тариф для расчёта стоимости."));
        return strategy.calculate(hall, hours);
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("Название съёмки обязательно.");
        }
        if (title.trim().length() > MAX_TITLE_LENGTH) {
            throw new BusinessException("Название слишком длинное (максимум " + MAX_TITLE_LENGTH + " символов).");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessException("Описание слишком длинное (максимум " + MAX_DESCRIPTION_LENGTH + " символов).");
        }
    }

    private void validateDuration(int hours) {
        if (hours < MIN_HOURS || hours > MAX_HOURS) {
            throw new BusinessException("Длительность съёмки должна быть от " + MIN_HOURS
                    + " до " + MAX_HOURS + " часов.");
        }
    }

    private void validateWorkingHours(LocalDateTime start, int hours) {
        LocalDateTime end = start.plusHours(hours);
        boolean sameDay = end.toLocalDate().equals(start.toLocalDate());
        if (start.toLocalTime().isBefore(STUDIO_OPEN) || !sameDay || end.toLocalTime().isAfter(STUDIO_CLOSE)) {
            throw new BusinessException("Студия работает с " + STUDIO_OPEN + " до " + STUDIO_CLOSE
                    + ". Съёмка (" + start.format(Formats.TIME) + "–" + end.format(Formats.TIME)
                    + ") должна целиком укладываться в это время.");
        }
    }

    private void validateNotInPast(LocalDateTime start) {
        if (start.isBefore(LocalDateTime.now())) {
            throw new BusinessException("Нельзя создать бронь на прошедшее время.");
        }
    }

    /** excludeId — ID брони, которую редактируем (она не должна конфликтовать сама с собой). */
    private void validateNoOverlap(int hallId, LocalDateTime start, int hours, int excludeId) {
        LocalDateTime end = start.plusHours(hours);
        boolean overlaps = bookingRepository.findByHallId(hallId).stream()
                .filter(b -> b.getId() != excludeId)
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .anyMatch(b -> start.isBefore(b.getEndTime()) && end.isAfter(b.getStartTime()));
        if (overlaps) {
            throw new BusinessException("Зал уже занят в это время. Выберите другое время или другой зал.");
        }
    }

    private String requireQuery(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("Поисковый запрос не может быть пустым.");
        }
        return text.trim();
    }

    private String normalize(String text) {
        return (text == null || text.isBlank()) ? null : text.trim();
    }
}
