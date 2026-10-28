package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.BookingService;
import ru.mirea.photostudio.service.ClientService;
import ru.mirea.photostudio.service.HallService;
import ru.mirea.photostudio.service.PhotographerService;

import java.time.LocalDateTime;
import java.util.List;

public class BookingMenu extends AbstractMenu {
    private final BookingService service;
    private final ClientService clients;
    private final PhotographerService photographers;
    private final HallService halls;

    public BookingMenu(ConsoleInput in, ConsolePrinter out, BookingService service, ClientService clients,
                       PhotographerService photographers, HallService halls) {
        super(in, out);
        this.service = service;
        this.clients = clients;
        this.photographers = photographers;
        this.halls = halls;
    }

    @Override
    protected String title() {
        return "БРОНИРОВАНИЯ";
    }

    @Override
    protected List<String> items() {
        return List.of("1. Создать бронирование", "2. Показать все бронирования", "3. Найти бронирование по ID",
                "4. Изменить бронирование", "5. Удалить бронирование", "6. Изменить статус бронирования");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> create();
            case "2" -> out.bookings(service.getAll());
            case "3" -> out.bookingDetails(service.getById(in.id("Введите ID: ")));
            case "4" -> update();
            case "5" -> {
                service.delete(in.id("Введите ID бронирования: "));
                System.out.println("Бронирование удалено.");
            }
            case "6" -> {
                long id = in.id("Введите ID бронирования: ");
                BookingStatus status = in.choose("Новый статус:", BookingStatus.values());
                Booking b = service.changeStatus(id, status);
                System.out.println("Статус изменён: " + b.getStatus().getTitle());
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    private void create() {
        System.out.println("--- Клиенты ---");
        out.clients(clients.getAll());
        long clientId = in.id("ID клиента: ");
        System.out.println("--- Фотографы ---");
        out.photographers(photographers.getAll());
        long photographerId = in.id("ID фотографа: ");
        System.out.println("--- Залы ---");
        out.halls(halls.getAll());
        long hallId = in.id("ID зала: ");
        SessionType type = in.choose("Тип съёмки:", SessionType.values());
        LocalDateTime start = in.dateTime("Начало (дд.мм.гггг чч:мм): ");
        LocalDateTime end = in.dateTime("Окончание (дд.мм.гггг чч:мм): ");
        int participants = in.integer("Количество участников: ", "Количество участников");
        String comment = in.line("Комментарий (можно пустой): ");
        Booking created = service.create(new Booking(clientId, photographerId, hallId, type, start, end,
                participants, comment));
        System.out.println("Бронирование создано, ID = " + created.getId()
                + ", стоимость: " + ConsolePrinter.money(created.getTotalPrice()));
    }

    private void update() {
        Booking b = service.getById(in.id("Введите ID бронирования: "));
        System.out.println("Enter — оставить значение без изменений. Клиента и статус менять нельзя.");
        Long photographerId = in.optionalId("ID фотографа [" + b.getPhotographerId() + "]: ");
        Long hallId = in.optionalId("ID зала [" + b.getHallId() + "]: ");
        SessionType type = in.chooseOptional("Тип съёмки [" + b.getSessionType().getTitle() + "]:", SessionType.values());
        LocalDateTime start = in.optionalDateTime("Начало [" + b.getStartTime() + "] (дд.мм.гггг чч:мм): ");
        LocalDateTime end = in.optionalDateTime("Окончание [" + b.getEndTime() + "] (дд.мм.гггг чч:мм): ");
        Integer participants = in.optionalInteger("Участников [" + b.getParticipants() + "]: ", "Количество участников");
        String comment = in.line("Комментарий [" + (b.getComment() == null ? "" : b.getComment()) + "]: ");
        if (photographerId != null) b.setPhotographerId(photographerId);
        if (hallId != null) b.setHallId(hallId);
        if (type != null) b.setSessionType(type);
        if (start != null) b.setStartTime(start);
        if (end != null) b.setEndTime(end);
        if (participants != null) b.setParticipants(participants);
        if (!comment.isEmpty()) b.setComment(comment);
        Booking updated = service.update(b);
        System.out.println("Бронирование обновлено, новая стоимость: " + ConsolePrinter.money(updated.getTotalPrice()));
    }
}
