package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.exception.ValidationException;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.PhotographerRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class PhotographerService {
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,15}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final BigDecimal MAX_RATE = new BigDecimal("100000");

    private final PhotographerRepository photographers;
    private final BookingRepository bookings;

    public PhotographerService(PhotographerRepository photographers, BookingRepository bookings) {
        this.photographers = photographers;
        this.bookings = bookings;
    }

    public Photographer create(Photographer photographer) {
        validate(photographer);
        return photographers.create(photographer);
    }

    public List<Photographer> getAll() {
        return photographers.findAll();
    }

    public Photographer getById(long id) {
        return photographers.findById(id).orElseThrow(() -> new EntityNotFoundException("Фотограф", id));
    }

    public Photographer update(Photographer photographer) {
        getById(photographer.getId());
        validate(photographer);
        photographers.update(photographer);
        return photographer;
    }

    public void delete(long id) {
        getById(id);
        if (bookings.existsByPhotographerId(id)) {
            throw new BusinessException("Нельзя удалить фотографа, у которого есть бронирования "
                    + "(можно отметить его как неработающего)");
        }
        photographers.deleteById(id);
    }

    public List<Photographer> search(String query) {
        String q = ClientService.normalizeQuery(query);
        return photographers.findAll().stream()
                .filter(p -> p.getFullName().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private void validate(Photographer p) {
        if (p.getFullName() == null || p.getFullName().isBlank()) {
            throw new ValidationException("ФИО фотографа не может быть пустым");
        }
        if (p.getFullName().trim().length() > 100) {
            throw new ValidationException("ФИО фотографа не должно превышать 100 символов");
        }
        p.setFullName(p.getFullName().trim());
        String phone = p.getPhone() == null ? "" : p.getPhone().replaceAll("[\\s()-]", "");
        if (!PHONE.matcher(phone).matches()) {
            throw new ValidationException("Телефон должен содержать от 10 до 15 цифр (допустим ведущий «+»)");
        }
        p.setPhone(phone);
        String email = p.getEmail() == null ? "" : p.getEmail().trim();
        if (!EMAIL.matcher(email).matches()) {
            throw new ValidationException("Некорректный email");
        }
        p.setEmail(email);
        if (p.getSpecialization() == null) {
            throw new ValidationException("Не указана специализация фотографа");
        }
        if (p.getHourlyRate() == null || p.getHourlyRate().signum() <= 0) {
            throw new ValidationException("Ставка фотографа должна быть больше нуля");
        }
        if (p.getHourlyRate().compareTo(MAX_RATE) > 0) {
            throw new ValidationException("Ставка фотографа не может превышать " + MAX_RATE);
        }
        for (Photographer other : photographers.findAll()) {
            if (other.getId() != p.getId()) {
                if (other.getPhone().equals(phone)) {
                    throw new BusinessException("Фотограф с таким телефоном уже существует");
                }
                if (other.getEmail().equalsIgnoreCase(email)) {
                    throw new BusinessException("Фотограф с таким email уже существует");
                }
            }
        }
    }
}
