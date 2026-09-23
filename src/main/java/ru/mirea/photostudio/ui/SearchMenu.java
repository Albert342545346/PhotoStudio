package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.service.BookingService;

import java.util.List;

/** Меню «Поиск»: 4 способа поиска бронирований (выполняются SQL-запросами с LIKE). */
public class SearchMenu extends Menu {

    private final BookingService bookingService;

    public SearchMenu(ConsoleInput in, BookingService bookingService) {
        super(in);
        this.bookingService = bookingService;
    }

    @Override
    protected String getTitle() {
        return "ПОИСК БРОНИРОВАНИЙ";
    }

    @Override
    protected List<String> getItems() {
        return List.of("По названию съёмки", "По описанию", "По имени клиента", "По дате съёмки");
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> printEntities("Результаты поиска",
                    bookingService.searchByTitle(in.readLine("Часть названия: ")));
            case 2 -> printEntities("Результаты поиска",
                    bookingService.searchByDescription(in.readLine("Часть описания: ")));
            case 3 -> printEntities("Результаты поиска",
                    bookingService.searchByClientName(in.readLine("Часть имени клиента: ")));
            case 4 -> printEntities("Результаты поиска",
                    bookingService.searchByDate(in.readDate("Дата съёмки")));
            default -> System.out.println("Неизвестный пункт меню.");
        }
    }
}
