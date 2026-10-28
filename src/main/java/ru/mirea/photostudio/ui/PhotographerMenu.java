package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.PhotographerService;

import java.math.BigDecimal;
import java.util.List;

public class PhotographerMenu extends AbstractMenu {
    private final PhotographerService service;

    public PhotographerMenu(ConsoleInput in, ConsolePrinter out, PhotographerService service) {
        super(in, out);
        this.service = service;
    }

    @Override
    protected String title() {
        return "ФОТОГРАФЫ";
    }

    @Override
    protected List<String> items() {
        return List.of("1. Добавить фотографа", "2. Показать всех фотографов", "3. Найти фотографа по ID",
                "4. Изменить фотографа", "5. Удалить фотографа");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> {
                String name = in.line("ФИО: ");
                String phone = in.line("Телефон (например, +79161234567): ");
                String email = in.line("Email: ");
                SessionType type = in.choose("Специализация:", SessionType.values());
                BigDecimal rate = in.decimal("Ставка за час, руб.: ", "Ставка");
                boolean active = in.yesNo("Сейчас работает?");
                Photographer created = service.create(new Photographer(name, phone, email, type, rate, active));
                System.out.println("Фотограф добавлен, ID = " + created.getId());
            }
            case "2" -> out.photographers(service.getAll());
            case "3" -> out.photographers(List.of(service.getById(in.id("Введите ID: "))));
            case "4" -> {
                Photographer p = service.getById(in.id("Введите ID фотографа: "));
                System.out.println("Enter — оставить значение без изменений.");
                String name = in.line("ФИО [" + p.getFullName() + "]: ");
                String phone = in.line("Телефон [" + p.getPhone() + "]: ");
                String email = in.line("Email [" + p.getEmail() + "]: ");
                SessionType type = in.chooseOptional("Специализация [" + p.getSpecialization().getTitle() + "]:",
                        SessionType.values());
                BigDecimal rate = in.optionalDecimal("Ставка за час [" + p.getHourlyRate() + "]: ", "Ставка");
                Boolean active = in.optionalYesNo("Работает? [" + (p.isActive() ? "да" : "нет") + "]");
                if (!name.isEmpty()) p.setFullName(name);
                if (!phone.isEmpty()) p.setPhone(phone);
                if (!email.isEmpty()) p.setEmail(email);
                if (type != null) p.setSpecialization(type);
                if (rate != null) p.setHourlyRate(rate);
                if (active != null) p.setActive(active);
                service.update(p);
                System.out.println("Фотограф обновлён.");
            }
            case "5" -> {
                service.delete(in.id("Введите ID фотографа: "));
                System.out.println("Фотограф удалён.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
