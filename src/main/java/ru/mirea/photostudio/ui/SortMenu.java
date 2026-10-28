package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.service.BookingService;

import java.util.List;

/** Сортировка бронирований: по дате начала, по стоимости, по дате создания. */
public class SortMenu extends AbstractMenu {
    private final BookingService bookings;

    public SortMenu(ConsoleInput in, ConsolePrinter out, BookingService bookings) {
        super(in, out);
        this.bookings = bookings;
    }

    @Override
    protected String title() {
        return "СОРТИРОВКА БРОНИРОВАНИЙ";
    }

    @Override
    protected List<String> items() {
        return List.of("1. По дате начала съёмки", "2. По стоимости", "3. По дате создания");
    }

    @Override
    protected boolean handle(String choice) {
        if (!List.of("1", "2", "3").contains(choice)) {
            return false;
        }
        boolean ascending = in.yesNo("По возрастанию?");
        switch (choice) {
            case "1" -> out.bookings(bookings.sortByStartTime(ascending));
            case "2" -> out.bookings(bookings.sortByPrice(ascending));
            default -> out.bookings(bookings.sortByCreatedAt(ascending));
        }
        return true;
    }
}
