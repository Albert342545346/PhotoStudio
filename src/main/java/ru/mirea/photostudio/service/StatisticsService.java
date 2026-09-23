package ru.mirea.photostudio.service;

import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.util.Formats;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/** Сбор статистики: часть показателей считает SQL (COUNT, GROUP BY), часть — Stream API. */
public class StatisticsService {

    private final BookingRepository bookingRepository;
    private final ClientRepository clientRepository;
    private final HallRepository hallRepository;

    public StatisticsService(BookingRepository bookingRepository, ClientRepository clientRepository,
                             HallRepository hallRepository) {
        this.bookingRepository = bookingRepository;
        this.clientRepository = clientRepository;
        this.hallRepository = hallRepository;
    }

    /** LinkedHashMap сохраняет порядок вставки — показатели выводятся в том порядке, в каком добавлены. */
    public Map<String, String> collect() {
        List<Booking> all = bookingRepository.findAll();
        Map<String, String> stats = new LinkedHashMap<>();

        stats.put("Всего клиентов", String.valueOf(clientRepository.count()));
        stats.put("Всего залов", String.valueOf(hallRepository.findAll().size()));
        stats.put("Всего бронирований", String.valueOf(all.size()));

        Map<BookingStatus, Integer> byStatus = bookingRepository.countByStatus();
        for (BookingStatus status : BookingStatus.values()) {
            stats.put("  " + status.getTitle(), String.valueOf(byStatus.get(status)));
        }

        stats.put("Выручка (завершённые съёмки)", Formats.money(sum(all, b -> b.getStatus() == BookingStatus.COMPLETED)));
        stats.put("Ожидаемая выручка (создан./подтв.)", Formats.money(sum(all, b -> b.getStatus().isActive())));
        stats.put("Средняя стоимость (без отменённых)", Formats.money(average(all)));

        stats.put("Самый популярный тип съёмки",
                mostFrequent(all, b -> b.getSessionType().getTitle()));
        stats.put("Самый загруженный зал", mostFrequent(all, Booking::getHallName));
        stats.put("Самый активный клиент", mostFrequent(all, Booking::getClientName));

        LocalDateTime now = LocalDateTime.now();
        stats.put("Ближайшая съёмка", all.stream()
                .filter(b -> b.getStatus().isActive() && b.getStartTime().isAfter(now))
                .min(Comparator.comparing(Booking::getStartTime))
                .map(b -> b.getTitle() + " — " + b.getStartTime().format(Formats.DATE_TIME))
                .orElse("нет запланированных"));
        return stats;
    }

    private BigDecimal sum(List<Booking> bookings, Predicate<Booking> filter) {
        return bookings.stream()
                .filter(filter)
                .map(Booking::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal average(List<Booking> bookings) {
        List<BigDecimal> prices = bookings.stream()
                .filter(b -> b.getStatus() != BookingStatus.CANCELLED)
                .map(Booking::getPrice)
                .toList();
        if (prices.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
    }

    /** Группирует брони по ключу, считает количество и возвращает самое частое значение. */
    private String mostFrequent(List<Booking> bookings, Function<Booking, String> keyExtractor) {
        return bookings.stream()
                .collect(Collectors.groupingBy(keyExtractor, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .orElse("нет данных");
    }
}
