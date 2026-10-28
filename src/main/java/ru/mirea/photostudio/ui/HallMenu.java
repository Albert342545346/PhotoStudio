package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.service.HallService;

import java.math.BigDecimal;
import java.util.List;

public class HallMenu extends AbstractMenu {
    private final HallService service;

    public HallMenu(ConsoleInput in, ConsolePrinter out, HallService service) {
        super(in, out);
        this.service = service;
    }

    @Override
    protected String title() {
        return "ЗАЛЫ";
    }

    @Override
    protected List<String> items() {
        return List.of("1. Добавить зал", "2. Показать все залы", "3. Найти зал по ID",
                "4. Изменить зал", "5. Удалить зал");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> {
                String name = in.line("Название: ");
                int capacity = in.integer("Вместимость (человек): ", "Вместимость");
                BigDecimal price = in.decimal("Цена за час, руб.: ", "Цена");
                String description = in.line("Описание (можно пустое): ");
                Hall created = service.create(new Hall(name, capacity, price, description));
                System.out.println("Зал добавлен, ID = " + created.getId());
            }
            case "2" -> out.halls(service.getAll());
            case "3" -> out.halls(List.of(service.getById(in.id("Введите ID: "))));
            case "4" -> {
                Hall h = service.getById(in.id("Введите ID зала: "));
                System.out.println("Enter — оставить значение без изменений.");
                String name = in.line("Название [" + h.getName() + "]: ");
                Integer capacity = in.optionalInteger("Вместимость [" + h.getCapacity() + "]: ", "Вместимость");
                BigDecimal price = in.optionalDecimal("Цена за час [" + h.getHourlyPrice() + "]: ", "Цена");
                String description = in.line("Описание [" + (h.getDescription() == null ? "" : h.getDescription()) + "]: ");
                if (!name.isEmpty()) h.setName(name);
                if (capacity != null) h.setCapacity(capacity);
                if (price != null) h.setHourlyPrice(price);
                if (!description.isEmpty()) h.setDescription(description);
                service.update(h);
                System.out.println("Зал обновлён.");
            }
            case "5" -> {
                service.delete(in.id("Введите ID зала: "));
                System.out.println("Зал удалён.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
