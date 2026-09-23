package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.BookingService;
import ru.mirea.photostudio.service.ClientService;
import ru.mirea.photostudio.service.HallService;

import java.time.LocalDateTime;
import java.util.List;

/** Меню «Бронирования»: CRUD основной сущности + смена статуса. */
public class BookingMenu extends Menu {

    private final BookingService bookingService;
    private final ClientService clientService;
    private final HallService hallService;

    public BookingMenu(ConsoleInput in, BookingService bookingService,
                       ClientService clientService, HallService hallService) {
        super(in);
        this.bookingService = bookingService;
        this.clientService = clientService;
        this.hallService = hallService;
    }

    @Override
    protected String getTitle() {
        return "БРОНИРОВАНИЯ ФОТОСЕССИЙ";
    }

    @Override
    protected List<String> getItems() {
        return List.of("Создать бронирование", "Показать все бронирования", "Найти бронирование по ID",
                "Изменить бронирование", "Изменить статус бронирования", "Удалить бронирование");
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> create();
            case 2 -> printEntities("Все бронирования", bookingService.getAll());
            case 3 -> showById();
            case 4 -> edit();
            case 5 -> changeStatus();
            case 6 -> delete();
            default -> System.out.println("Неизвестный пункт меню.");
        }
    }

    private void create() {
        printEntities("Клиенты", clientService.getAll());
        int clientId = in.readId("Введите ID клиента: ");
        printEntities("Залы", hallService.getAll());
        int hallId = in.readId("Введите ID зала: ");
        String title = in.readLine("Название съёмки: ");
        String description = in.readLine("Описание (можно оставить пустым): ");
        SessionType type = in.readEnum("Тип съёмки", SessionType.values(), null);
        LocalDateTime start = in.readDateTime("Дата и время начала", null);
        int hours = in.readInt("Длительность (часов, 1–8): ", "Длительность должна быть целым числом.");

        Booking booking = bookingService.create(clientId, hallId, title, description, type, start, hours);
        System.out.println("Бронирование создано. Стоимость рассчитана автоматически:");
        System.out.println(booking.describe());
    }

    private void showById() {
        int id = in.readId("Введите ID: ");
        System.out.println(bookingService.getById(id).describe());
    }

    private void edit() {
        int id = in.readId("Введите ID бронирования: ");
        Booking current = bookingService.getById(id);
        System.out.println("Текущие данные:");
        System.out.println(current.describe());
        System.out.println("(Enter — оставить значение без изменений)");

        String title = in.readLine("Название", current.getTitle());
        String currentDescription = current.getDescription() == null ? "" : current.getDescription();
        String description = in.readLine("Описание", currentDescription);
        int hallId = in.readInt("ID зала", "ID должен быть целым числом.", current.getHallId());
        SessionType type = in.readEnum("Тип съёмки", SessionType.values(), current.getSessionType());
        LocalDateTime start = in.readDateTime("Дата и время начала", current.getStartTime());
        int hours = in.readInt("Длительность (часов)", "Длительность должна быть целым числом.",
                current.getDurationHours());

        Booking updated = bookingService.update(id, hallId, title, description, type, start, hours);
        System.out.println("Бронирование изменено:");
        System.out.println(updated.describe());
    }

    private void changeStatus() {
        int id = in.readId("Введите ID бронирования: ");
        Booking current = bookingService.getById(id);
        System.out.println("Текущий статус: " + current.getStatus().getTitle());
        BookingStatus newStatus = in.readEnum("Новый статус", BookingStatus.values(), null);
        Booking updated = bookingService.changeStatus(id, newStatus);
        System.out.println("Статус изменён на «" + updated.getStatus().getTitle() + "».");
    }

    private void delete() {
        int id = in.readId("Введите ID бронирования: ");
        Booking booking = bookingService.getById(id);
        System.out.println(booking.describe());
        if (in.confirm("Удалить это бронирование?")) {
            bookingService.delete(id);
            System.out.println("Бронирование удалено.");
        } else {
            System.out.println("Удаление отменено.");
        }
    }
}
