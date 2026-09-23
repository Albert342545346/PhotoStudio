package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.service.ClientService;

import java.util.List;

/** Меню «Клиенты»: CRUD клиентов. */
public class ClientMenu extends Menu {

    private final ClientService clientService;

    public ClientMenu(ConsoleInput in, ClientService clientService) {
        super(in);
        this.clientService = clientService;
    }

    @Override
    protected String getTitle() {
        return "КЛИЕНТЫ";
    }

    @Override
    protected List<String> getItems() {
        return List.of("Добавить клиента", "Показать всех клиентов", "Найти клиента по ID",
                "Изменить клиента", "Удалить клиента");
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> create();
            case 2 -> printEntities("Все клиенты", clientService.getAll());
            case 3 -> showById();
            case 4 -> edit();
            case 5 -> delete();
            default -> System.out.println("Неизвестный пункт меню.");
        }
    }

    private void create() {
        String name = in.readLine("ФИО: ");
        String phone = in.readLine("Телефон (например, +79161234567): ");
        String email = in.readLine("Email: ");
        Client client = clientService.create(name, phone, email);
        System.out.println("Клиент добавлен:");
        System.out.println(client.describe());
    }

    private void showById() {
        int id = in.readId("Введите ID: ");
        System.out.println(clientService.getById(id).describe());
    }

    private void edit() {
        int id = in.readId("Введите ID клиента: ");
        Client current = clientService.getById(id);
        System.out.println("Текущие данные: " + current.describe());
        System.out.println("(Enter — оставить значение без изменений)");
        String name = in.readLine("ФИО", current.getFullName());
        String phone = in.readLine("Телефон", current.getPhone());
        String email = in.readLine("Email", current.getEmail());
        Client updated = clientService.update(id, name, phone, email);
        System.out.println("Клиент изменён:");
        System.out.println(updated.describe());
    }

    private void delete() {
        int id = in.readId("Введите ID клиента: ");
        Client client = clientService.getById(id);
        System.out.println(client.describe());
        if (in.confirm("Удалить этого клиента?")) {
            clientService.delete(id);
            System.out.println("Клиент удалён.");
        } else {
            System.out.println("Удаление отменено.");
        }
    }
}
