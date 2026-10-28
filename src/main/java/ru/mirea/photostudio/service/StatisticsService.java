package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.repository.PaymentRepository;
import ru.mirea.photostudio.repository.PhotographerRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Подсчёт статистики системы (больше пяти показателей). */
public class StatisticsService {
    private final ClientRepository clients;
    private final PhotographerRepository photographers;
    private final HallRepository halls;
    private final BookingRepository bookings;
    private final PaymentRepository payments;

    public StatisticsService(ClientRepository clients, PhotographerRepository photographers, HallRepository halls,
                             BookingRepository bookings, PaymentRepository payments) {
        this.clients = clients;
        this.photographers = photographers;
        this.halls = halls;
        this.bookings = bookings;
        this.payments = payments;
    }

    public Map<String, String> collect() {
        List<Client> allClients = clients.findAll();
        List<Booking> allBookings = bookings.findAll();
        LocalDateTime now = LocalDateTime.now();

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Всего клиентов", String.valueOf(allClients.size()));
        stats.put("Из них VIP-клиентов", String.valueOf(allClients.stream().filter(Client::isVip).count()));
        stats.put("Всего фотографов", String.valueOf(photographers.findAll().size()));
        stats.put("Всего залов", String.valueOf(halls.findAll().size()));
        stats.put("Всего бронирований", String.valueOf(allBookings.size()));
        stats.put("Активных бронирований (создано/подтверждено)",
                String.valueOf(allBookings.stream().filter(b -> b.getStatus().isActive()).count()));
        stats.put("Завершённых бронирований", String.valueOf(countByStatus(allBookings, BookingStatus.COMPLETED)));
        stats.put("Отменённых бронирований", String.valueOf(countByStatus(allBookings, BookingStatus.CANCELLED)));
        stats.put("Предстоящих бронирований на ближайшие 7 дней", String.valueOf(allBookings.stream()
                .filter(b -> b.getStatus().isActive())
                .filter(b -> b.getStartTime().isAfter(now) && b.getStartTime().isBefore(now.plusDays(7)))
                .count()));
        stats.put("Выручка (сумма платежей), руб.", payments.sumAll().toPlainString());
        stats.put("Средняя стоимость бронирования, руб.", averagePrice(allBookings).toPlainString());
        stats.put("Минимальная стоимость бронирования, руб.", extremePrice(allBookings, true));
        stats.put("Максимальная стоимость бронирования, руб.", extremePrice(allBookings, false));
        stats.put("Доля VIP-клиентов, %", allClients.isEmpty() ? "0"
                : String.valueOf(Math.round(allClients.stream().filter(Client::isVip).count() * 100.0 / allClients.size())));
        stats.put("Самый популярный тип съёмки", mostPopularType(allBookings));
        return stats;
    }

    /** Количество бронирований по каждому статусу (для диаграммы). */
    public Map<BookingStatus, Long> countByStatus() {
        Map<BookingStatus, Long> result = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            result.put(status, 0L);
        }
        for (Booking b : bookings.findAll()) {
            result.merge(b.getStatus(), 1L, Long::sum);
        }
        return result;
    }

    /** Количество неотменённых бронирований по типам съёмки (для диаграммы). */
    public Map<SessionType, Long> countBySessionType() {
        Map<SessionType, Long> result = new EnumMap<>(SessionType.class);
        for (SessionType type : SessionType.values()) {
            result.put(type, 0L);
        }
        for (Booking b : bookings.findAll()) {
            if (b.getStatus() != BookingStatus.CANCELLED) {
                result.merge(b.getSessionType(), 1L, Long::sum);
            }
        }
        return result;
    }

    /** Количество бронирований, начинающихся в выбранный период (границы включительно). */
    public long countBookingsBetween(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException("Укажите обе даты периода");
        }
        if (from.isAfter(to)) {
            throw new BusinessException("Дата «с» не может быть позже даты «по»");
        }
        return bookings.findAll().stream()
                .filter(b -> !b.getStartTime().toLocalDate().isBefore(from)
                        && !b.getStartTime().toLocalDate().isAfter(to))
                .count();
    }

    private String extremePrice(List<Booking> list, boolean min) {
        return list.stream().map(Booking::getTotalPrice)
                .reduce((a, b) -> (min ? a.min(b) : a.max(b)))
                .map(BigDecimal::toPlainString).orElse("0.00");
    }

    private long countByStatus(List<Booking> list, BookingStatus status) {
        return list.stream().filter(b -> b.getStatus() == status).count();
    }

    private BigDecimal averagePrice(List<Booking> list) {
        if (list.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal sum = list.stream().map(Booking::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
    }

    private String mostPopularType(List<Booking> list) {
        Map<SessionType, Integer> counts = new EnumMap<>(SessionType.class);
        for (Booking b : list) {
            if (b.getStatus() != BookingStatus.CANCELLED) {
                counts.merge(b.getSessionType(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey().getTitle() + " (" + e.getValue() + ")")
                .orElse("нет данных");
    }
}
