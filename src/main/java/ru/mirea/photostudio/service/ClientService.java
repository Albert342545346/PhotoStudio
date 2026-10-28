package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.exception.ValidationException;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ClientService {
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final ClientRepository clients;
    private final BookingRepository bookings;

    public ClientService(ClientRepository clients, BookingRepository bookings) {
        this.clients = clients;
        this.bookings = bookings;
    }

    public Client create(Client client) {
        validate(client);
        return clients.create(client);
    }

    public List<Client> getAll() {
        return clients.findAll();
    }

    public Client getById(long id) {
        return clients.findById(id).orElseThrow(() -> new EntityNotFoundException("Клиент", id));
    }

    public Client update(Client client) {
        getById(client.getId());
        validate(client);
        clients.update(client);
        return client;
    }

    public void delete(long id) {
        getById(id);
        if (bookings.existsByClientId(id)) {
            throw new BusinessException("Нельзя удалить клиента, у которого есть бронирования");
        }
        clients.deleteById(id);
    }

    /** Поиск по части ФИО, телефона или email (без учёта регистра). */
    public List<Client> search(String query) {
        String q = normalizeQuery(query);
        return clients.findAll().stream()
                .filter(c -> c.getFullName().toLowerCase(Locale.ROOT).contains(q)
                        || c.getPhone().contains(q)
                        || c.getEmail().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            throw new ValidationException("Строка поиска не должна быть пустой");
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private void validate(Client c) {
        if (c.getFullName() == null || c.getFullName().isBlank()) {
            throw new ValidationException("ФИО клиента не может быть пустым");
        }
        if (c.getFullName().trim().length() > 100) {
            throw new ValidationException("ФИО клиента не должно превышать 100 символов");
        }
        c.setFullName(c.getFullName().trim());
        String phone = c.getPhone() == null ? "" : c.getPhone().replaceAll("[\\s()-]", "");
        if (!PHONE.matcher(phone).matches()) {
            throw new ValidationException("Телефон должен содержать от 10 до 15 цифр (допустим ведущий «+»)");
        }
        c.setPhone(phone);
        String email = c.getEmail() == null ? "" : c.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Некорректный email");
        }
        c.setEmail(email);
        for (Client other : clients.findAll()) {
            if (other.getId() != c.getId()) {
                if (other.getPhone().equals(phone)) {
                    throw new BusinessException("Клиент с таким телефоном уже существует");
                }
                if (other.getEmail().equalsIgnoreCase(email)) {
                    throw new BusinessException("Клиент с таким email уже существует");
                }
            }
        }
    }
}
