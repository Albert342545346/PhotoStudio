package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.ValidationException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.repository.PaymentRepository;
import ru.mirea.photostudio.repository.PhotographerRepository;
import ru.mirea.photostudio.util.ExcelExporter;
import ru.mirea.photostudio.util.ExcelExporter.SheetData;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Подготавливает данные из БД и передаёт их ExcelExporter. */
public class ExportService {
    private final ClientRepository clients;
    private final PhotographerRepository photographers;
    private final HallRepository halls;
    private final BookingRepository bookings;
    private final PaymentRepository payments;
    private final ExcelExporter exporter;

    public ExportService(ClientRepository clients, PhotographerRepository photographers, HallRepository halls,
                         BookingRepository bookings, PaymentRepository payments, ExcelExporter exporter) {
        this.clients = clients;
        this.photographers = photographers;
        this.halls = halls;
        this.bookings = bookings;
        this.payments = payments;
        this.exporter = exporter;
    }

    /** Экспорт всех таблиц в один Excel-файл (по листу на таблицу). */
    public Path exportToExcel(Path file) {
        Map<String, SheetData> sheets = new LinkedHashMap<>();
        sheets.put("Клиенты", clientsSheet());
        sheets.put("Фотографы", photographersSheet());
        sheets.put("Залы", hallsSheet());
        sheets.put("Бронирования", bookingsSheet(bookings.findAll()));
        sheets.put("Платежи", paymentsSheet());
        exporter.exportXlsx(file, sheets);
        return file.toAbsolutePath();
    }

    /** Дополнительный экспорт бронирований в CSV. */
    public Path exportBookingsToCsv(Path file) {
        exporter.exportCsv(file, bookingsSheet(bookings.findAll()));
        return file.toAbsolutePath();
    }

    /** Экспорт переданного списка бронирований (например, отфильтрованного в таблице). Формат — по расширению файла. */
    public Path exportBookings(List<Booking> list, Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".csv")) {
            exporter.exportCsv(file, bookingsSheet(list));
        } else if (name.endsWith(".xlsx")) {
            exporter.exportXlsx(file, Map.of("Бронирования", bookingsSheet(list)));
        } else {
            throw new ValidationException("Поддерживаются только форматы .xlsx и .csv");
        }
        return file.toAbsolutePath();
    }

    private SheetData clientsSheet() {
        List<List<Object>> rows = new ArrayList<>();
        for (Client c : clients.findAll()) {
            rows.add(List.of(c.getId(), c.getFullName(), c.getPhone(), c.getEmail(), c.isVip(), c.getRegisteredAt()));
        }
        return new SheetData(List.of("ID", "ФИО", "Телефон", "Email", "VIP", "Дата регистрации"), rows);
    }

    private SheetData photographersSheet() {
        List<List<Object>> rows = new ArrayList<>();
        for (Photographer p : photographers.findAll()) {
            rows.add(List.of(p.getId(), p.getFullName(), p.getPhone(), p.getEmail(),
                    p.getSpecialization().getTitle(), p.getHourlyRate(), p.isActive()));
        }
        return new SheetData(List.of("ID", "ФИО", "Телефон", "Email", "Специализация", "Ставка в час", "Работает"), rows);
    }

    private SheetData hallsSheet() {
        List<List<Object>> rows = new ArrayList<>();
        for (Hall h : halls.findAll()) {
            rows.add(List.of(h.getId(), h.getName(), h.getCapacity(), h.getHourlyPrice(),
                    h.getDescription() == null ? "" : h.getDescription()));
        }
        return new SheetData(List.of("ID", "Название", "Вместимость", "Цена в час", "Описание"), rows);
    }

    private SheetData bookingsSheet(List<Booking> list) {
        Map<Long, String> clientNames = clients.findAll().stream()
                .collect(Collectors.toMap(Client::getId, Client::getFullName));
        Map<Long, String> photographerNames = photographers.findAll().stream()
                .collect(Collectors.toMap(Photographer::getId, Photographer::getFullName));
        Map<Long, String> hallNames = halls.findAll().stream()
                .collect(Collectors.toMap(Hall::getId, Hall::getName));
        List<List<Object>> rows = new ArrayList<>();
        for (Booking b : list) {
            rows.add(List.of(b.getId(),
                    nameOf(clientNames, b.getClientId()),
                    nameOf(photographerNames, b.getPhotographerId()),
                    nameOf(hallNames, b.getHallId()),
                    b.getSessionType().getTitle(), b.getStatus().getTitle(),
                    b.getStartTime(), b.getEndTime(), b.getParticipants(), b.getTotalPrice(),
                    b.getComment() == null ? "" : b.getComment()));
        }
        return new SheetData(List.of("ID", "Клиент", "Фотограф", "Зал", "Тип съёмки", "Статус", "Начало",
                "Окончание", "Участников", "Стоимость", "Комментарий"), rows);
    }

    private SheetData paymentsSheet() {
        List<List<Object>> rows = new ArrayList<>();
        for (Payment p : payments.findAll()) {
            rows.add(List.of(p.getId(), p.getBookingId(), p.getAmount(), p.getMethod().getTitle(), p.getPaidAt()));
        }
        return new SheetData(List.of("ID", "Бронирование", "Сумма", "Способ оплаты", "Дата платежа"), rows);
    }

    private static String nameOf(Map<Long, String> names, long id) {
        return names.getOrDefault(id, "ID " + id);
    }
}
