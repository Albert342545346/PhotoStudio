package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.BookingService;

import java.time.LocalDate;
import java.util.List;

/** Фильтрация бронирований: по статусу, по типу съёмки, по диапазону дат. */
public class FilterMenu extends AbstractMenu {
    private final BookingService bookings;

    public FilterMenu(ConsoleInput in, ConsolePrinter out, BookingService bookings) {
        super(in, out);
        this.bookings = bookings;
    }

    @Override
    protected String title() {
        return "ФИЛЬТРАЦИЯ БРОНИРОВАНИЙ";
    }

    @Override
    protected List<String> items() {
        return List.of("1. По статусу", "2. По типу съёмки", "3. По диапазону дат");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> out.bookings(bookings.filterByStatus(in.choose("Статус:", BookingStatus.values())));
            case "2" -> out.bookings(bookings.filterBySessionType(in.choose("Тип съёмки:", SessionType.values())));
            case "3" -> {
                LocalDate from = in.date("Дата начала диапазона (дд.мм.гггг): ");
                LocalDate to = in.date("Дата конца диапазона (дд.мм.гггг): ");
                out.bookings(bookings.filterByDateRange(from, to));
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
