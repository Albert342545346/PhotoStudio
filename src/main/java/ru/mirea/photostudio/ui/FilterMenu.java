package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.BookingService;
import ru.mirea.photostudio.service.HallService;

import java.time.LocalDate;
import java.util.List;

/** Меню «Фильтрация»: 4 фильтра (реализованы через Stream API в сервисе). */
public class FilterMenu extends Menu {

    private final BookingService bookingService;
    private final HallService hallService;

    public FilterMenu(ConsoleInput in, BookingService bookingService, HallService hallService) {
        super(in);
        this.bookingService = bookingService;
        this.hallService = hallService;
    }

    @Override
    protected String getTitle() {
        return "ФИЛЬТРАЦИЯ БРОНИРОВАНИЙ";
    }

    @Override
    protected List<String> getItems() {
        return List.of("По статусу", "По типу съёмки", "По диапазону дат", "По залу");
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> {
                BookingStatus status = in.readEnum("Статус", BookingStatus.values(), null);
                printEntities("Брони со статусом «" + status.getTitle() + "»",
                        bookingService.filterByStatus(status));
            }
            case 2 -> {
                SessionType type = in.readEnum("Тип съёмки", SessionType.values(), null);
                printEntities("Брони типа «" + type.getTitle() + "»",
                        bookingService.filterBySessionType(type));
            }
            case 3 -> {
                LocalDate from = in.readDate("Начальная дата");
                LocalDate to = in.readDate("Конечная дата");
                printEntities("Брони за период", bookingService.filterByDateRange(from, to));
            }
            case 4 -> {
                printEntities("Залы", hallService.getAll());
                int hallId = in.readId("Введите ID зала: ");
                printEntities("Брони зала", bookingService.filterByHall(hallId));
            }
            default -> System.out.println("Неизвестный пункт меню.");
        }
    }
}
