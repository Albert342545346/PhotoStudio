package ru.mirea.photostudio.model;

import java.util.Comparator;

/**
 * Способы сортировки бронирований. Каждое значение enum хранит свой Comparator,
 * поэтому сервису не нужен switch: достаточно вызвать stream.sorted(sort.getComparator()).
 */
public enum BookingSort implements Titled {
    BY_START_TIME("По дате и времени съёмки (сначала ранние)",
            Comparator.comparing(Booking::getStartTime)),
    BY_PRICE_DESC("По стоимости (сначала дорогие)",
            Comparator.comparing(Booking::getPrice).reversed()),
    BY_CLIENT("По имени клиента (А-Я)",
            Comparator.comparing(Booking::getClientName, String.CASE_INSENSITIVE_ORDER)),
    BY_TITLE("По названию (А-Я)",
            Comparator.comparing(Booking::getTitle, String.CASE_INSENSITIVE_ORDER));

    private final String title;
    private final Comparator<Booking> comparator;

    BookingSort(String title, Comparator<Booking> comparator) {
        this.title = title;
        this.comparator = comparator;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public Comparator<Booking> getComparator() {
        return comparator;
    }
}
