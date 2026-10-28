package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.service.BookingService;
import ru.mirea.photostudio.service.ClientService;
import ru.mirea.photostudio.service.PhotographerService;

import java.util.List;

/** Поиск: клиенты, фотографы, бронирования (по клиенту, фотографу, дате). */
public class SearchMenu extends AbstractMenu {
    private final ClientService clients;
    private final PhotographerService photographers;
    private final BookingService bookings;

    public SearchMenu(ConsoleInput in, ConsolePrinter out, ClientService clients,
                      PhotographerService photographers, BookingService bookings) {
        super(in, out);
        this.clients = clients;
        this.photographers = photographers;
        this.bookings = bookings;
    }

    @Override
    protected String title() {
        return "ПОИСК";
    }

    @Override
    protected List<String> items() {
        return List.of("1. Клиенты (по ФИО, телефону или email)", "2. Фотографы (по ФИО)",
                "3. Бронирования по имени клиента", "4. Бронирования по имени фотографа",
                "5. Бронирования по дате");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> out.clients(clients.search(in.line("Введите ФИО, телефон или email: ")));
            case "2" -> out.photographers(photographers.search(in.line("Введите ФИО: ")));
            case "3" -> out.bookings(bookings.searchByClientName(in.line("Введите ФИО клиента: ")));
            case "4" -> out.bookings(bookings.searchByPhotographerName(in.line("Введите ФИО фотографа: ")));
            case "5" -> out.bookings(bookings.searchByDate(in.date("Введите дату (дд.мм.гггг): ")));
            default -> {
                return false;
            }
        }
        return true;
    }
}
