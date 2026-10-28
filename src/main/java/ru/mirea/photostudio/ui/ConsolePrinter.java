package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.service.ClientService;
import ru.mirea.photostudio.service.HallService;
import ru.mirea.photostudio.service.PaymentService;
import ru.mirea.photostudio.service.PhotographerService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Форматированный вывод сущностей в консоль. */
public class ConsolePrinter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ClientService clients;
    private final PhotographerService photographers;
    private final HallService halls;
    private final PaymentService payments;

    public ConsolePrinter(ClientService clients, PhotographerService photographers, HallService halls,
                          PaymentService payments) {
        this.clients = clients;
        this.photographers = photographers;
        this.halls = halls;
        this.payments = payments;
    }

    public void clients(List<Client> list) {
        if (isEmpty(list)) {
            return;
        }
        System.out.printf("%-4s %-30s %-14s %-28s %-12s%n", "ID", "ФИО", "Телефон", "Email", "Статус");
        for (Client c : list) {
            System.out.printf("%-4d %-30s %-14s %-28s %-12s%n", c.getId(), cut(c.getFullName(), 30),
                    c.getPhone(), cut(c.getEmail(), 28), c.getRoleTitle());
        }
    }

    public void photographers(List<Photographer> list) {
        if (isEmpty(list)) {
            return;
        }
        System.out.printf("%-4s %-30s %-14s %-15s %-10s %-12s%n", "ID", "ФИО", "Телефон", "Специализация",
                "Ставка/ч", "Работает");
        for (Photographer p : list) {
            System.out.printf("%-4d %-30s %-14s %-15s %-10s %-12s%n", p.getId(), cut(p.getFullName(), 30),
                    p.getPhone(), p.getSpecialization().getTitle(), money(p.getHourlyRate()),
                    p.isActive() ? "да" : "нет");
        }
    }

    public void halls(List<Hall> list) {
        if (isEmpty(list)) {
            return;
        }
        System.out.printf("%-4s %-22s %-12s %-10s %s%n", "ID", "Название", "Вместимость", "Цена/ч", "Описание");
        for (Hall h : list) {
            System.out.printf("%-4d %-22s %-12d %-10s %s%n", h.getId(), cut(h.getName(), 22), h.getCapacity(),
                    money(h.getHourlyPrice()), h.getDescription() == null ? "" : cut(h.getDescription(), 40));
        }
    }

    public void bookings(List<Booking> list) {
        if (isEmpty(list)) {
            return;
        }
        Map<Long, String> clientNames = names(clients.getAll(), Client::getId, Client::getFullName);
        Map<Long, String> photographerNames = names(photographers.getAll(), Photographer::getId, Photographer::getFullName);
        Map<Long, String> hallNames = names(halls.getAll(), Hall::getId, Hall::getName);
        System.out.printf("%-4s %-20s %-20s %-14s %-14s %-13s %-16s %-16s %-4s %-11s%n", "ID", "Клиент", "Фотограф",
                "Зал", "Тип", "Статус", "Начало", "Окончание", "Чел.", "Стоимость");
        for (Booking b : list) {
            System.out.printf("%-4d %-20s %-20s %-14s %-14s %-13s %-16s %-16s %-4d %-11s%n", b.getId(),
                    cut(clientNames.getOrDefault(b.getClientId(), "ID " + b.getClientId()), 20),
                    cut(photographerNames.getOrDefault(b.getPhotographerId(), "ID " + b.getPhotographerId()), 20),
                    cut(hallNames.getOrDefault(b.getHallId(), "ID " + b.getHallId()), 14),
                    b.getSessionType().getTitle(), b.getStatus().getTitle(),
                    time(b.getStartTime()), time(b.getEndTime()), b.getParticipants(), money(b.getTotalPrice()));
        }
    }

    public void bookingDetails(Booking b) {
        System.out.println("Бронирование №" + b.getId());
        System.out.println("  Клиент:        " + clients.getById(b.getClientId()).getFullName());
        System.out.println("  Фотограф:      " + photographers.getById(b.getPhotographerId()).getFullName());
        System.out.println("  Зал:           " + halls.getById(b.getHallId()).getName());
        System.out.println("  Тип съёмки:    " + b.getSessionType().getTitle());
        System.out.println("  Статус:        " + b.getStatus().getTitle());
        System.out.println("  Начало:        " + time(b.getStartTime()));
        System.out.println("  Окончание:     " + time(b.getEndTime()));
        System.out.println("  Участников:    " + b.getParticipants());
        System.out.println("  Стоимость:     " + money(b.getTotalPrice()));
        System.out.println("  Остаток оплаты:" + " " + money(payments.remainingToPay(b)));
        System.out.println("  Комментарий:   " + (b.getComment() == null || b.getComment().isBlank() ? "—" : b.getComment()));
        System.out.println("  Создано:       " + time(b.getCreatedAt()));
    }

    public void payments(List<Payment> list) {
        if (isEmpty(list)) {
            return;
        }
        System.out.printf("%-4s %-13s %-12s %-20s %-16s%n", "ID", "Бронирование", "Сумма", "Способ оплаты", "Дата");
        for (Payment p : list) {
            System.out.printf("%-4d %-13d %-12s %-20s %-16s%n", p.getId(), p.getBookingId(), money(p.getAmount()),
                    p.getMethod().getTitle(), time(p.getPaidAt()));
        }
    }

    private <T> Map<Long, String> names(List<T> list, Function<T, Long> id, Function<T, String> name) {
        return list.stream().collect(Collectors.toMap(id, name));
    }

    private boolean isEmpty(List<?> list) {
        if (list.isEmpty()) {
            System.out.println("Записей не найдено.");
            return true;
        }
        return false;
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private static String time(LocalDateTime dateTime) {
        return dateTime == null ? "—" : DATE_TIME.format(dateTime);
    }

    public static String money(BigDecimal value) {
        return value == null ? "—" : value.setScale(2, java.math.RoundingMode.HALF_UP) + " ₽";
    }
}
