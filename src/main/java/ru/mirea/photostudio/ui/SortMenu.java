package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.BookingSort;
import ru.mirea.photostudio.model.Titled;
import ru.mirea.photostudio.service.BookingService;

import java.util.Arrays;
import java.util.List;

/** Меню «Сортировка». Пункты строятся автоматически из значений enum BookingSort. */
public class SortMenu extends Menu {

    private final BookingService bookingService;

    public SortMenu(ConsoleInput in, BookingService bookingService) {
        super(in);
        this.bookingService = bookingService;
    }

    @Override
    protected String getTitle() {
        return "СОРТИРОВКА БРОНИРОВАНИЙ";
    }

    @Override
    protected List<String> getItems() {
        return Arrays.stream(BookingSort.values()).map(Titled::getTitle).toList();
    }

    @Override
    protected void handle(int choice) {
        BookingSort sort = BookingSort.values()[choice - 1];
        printEntities("Бронирования: " + sort.getTitle().toLowerCase(), bookingService.sort(sort));
    }
}
