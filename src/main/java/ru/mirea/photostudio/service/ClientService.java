package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;

import java.util.List;
import java.util.regex.Pattern;

/** Бизнес-логика работы с клиентами: проверки данных, уникальность email, защита от удаления. */
public class ClientService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?\\d{10,15}$");

    private final ClientRepository clientRepository;
    private final BookingRepository bookingRepository;

    public ClientService(ClientRepository clientRepository, BookingRepository bookingRepository) {
        this.clientRepository = clientRepository;
        this.bookingRepository = bookingRepository;
    }

    public Client create(String fullName, String phone, String email) {
        String normalizedPhone = normalizePhone(phone);
        validate(fullName, normalizedPhone, email);
        if (clientRepository.findByEmail(email.trim()).isPresent()) {
            throw new BusinessException("Клиент с email " + email.trim() + " уже существует.");
        }
        return clientRepository.save(new Client(fullName.trim(), normalizedPhone, email.trim().toLowerCase()));
    }

    public List<Client> getAll() {
        return clientRepository.findAll();
    }

    public Client getById(int id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент", id));
    }

    public Client update(int id, String fullName, String phone, String email) {
        Client client = getById(id);
        String normalizedPhone = normalizePhone(phone);
        validate(fullName, normalizedPhone, email);
        boolean emailTaken = clientRepository.findByEmail(email.trim())
                .filter(other -> other.getId() != id)
                .isPresent();
        if (emailTaken) {
            throw new BusinessException("Email " + email.trim() + " уже используется другим клиентом.");
        }
        client.setFullName(fullName.trim());
        client.setPhone(normalizedPhone);
        client.setEmail(email.trim().toLowerCase());
        clientRepository.update(client);
        return client;
    }

    public void delete(int id) {
        getById(id); // если клиента нет — будет EntityNotFoundException
        if (bookingRepository.existsByClientId(id)) {
            throw new BusinessException("Нельзя удалить клиента, у которого есть бронирования.");
        }
        clientRepository.deleteById(id);
    }

    public List<Client> searchByName(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("Поисковый запрос не может быть пустым.");
        }
        return clientRepository.searchByName(text.trim());
    }

    // ---------------------------------------------------------- проверки

    private void validate(String fullName, String phone, String email) {
        if (fullName == null || fullName.trim().length() < 2) {
            throw new BusinessException("ФИО клиента обязательно (минимум 2 символа).");
        }
        if (fullName.trim().length() > 100) {
            throw new BusinessException("ФИО слишком длинное (максимум 100 символов).");
        }
        if (!PHONE.matcher(phone).matches()) {
            throw new BusinessException("Некорректный телефон. Пример: +79161234567 (10–15 цифр).");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new BusinessException("Некорректный email. Пример: name@mail.ru");
        }
    }

    /** Убирает пробелы, дефисы и скобки: «+7 (916) 123-45-67» -> «+79161234567». */
    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[\\s\\-()]", "");
    }
}
