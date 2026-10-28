package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.service.ClientService;

import java.util.List;

public class ClientMenu extends AbstractMenu {
    private final ClientService service;

    public ClientMenu(ConsoleInput in, ConsolePrinter out, ClientService service) {
        super(in, out);
        this.service = service;
    }

    @Override
    protected String title() {
        return "КЛИЕНТЫ";
    }

    @Override
    protected List<String> items() {
        return List.of("1. Добавить клиента", "2. Показать всех клиентов", "3. Найти клиента по ID",
                "4. Изменить клиента", "5. Удалить клиента");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> {
                String name = in.line("ФИО: ");
                String phone = in.line("Телефон (например, +79161234567): ");
                String email = in.line("Email: ");
                boolean vip = in.yesNo("VIP-клиент?");
                Client created = service.create(new Client(name, phone, email, vip));
                System.out.println("Клиент добавлен, ID = " + created.getId());
            }
            case "2" -> out.clients(service.getAll());
            case "3" -> out.clients(List.of(service.getById(in.id("Введите ID: "))));
            case "4" -> {
                Client c = service.getById(in.id("Введите ID клиента: "));
                System.out.println("Enter — оставить значение без изменений.");
                String name = in.line("ФИО [" + c.getFullName() + "]: ");
                String phone = in.line("Телефон [" + c.getPhone() + "]: ");
                String email = in.line("Email [" + c.getEmail() + "]: ");
                Boolean vip = in.optionalYesNo("VIP-клиент? [" + (c.isVip() ? "да" : "нет") + "]");
                if (!name.isEmpty()) c.setFullName(name);
                if (!phone.isEmpty()) c.setPhone(phone);
                if (!email.isEmpty()) c.setEmail(email);
                if (vip != null) c.setVip(vip);
                service.update(c);
                System.out.println("Клиент обновлён.");
            }
            case "5" -> {
                service.delete(in.id("Введите ID клиента: "));
                System.out.println("Клиент удалён.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
