package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.exception.ValidationException;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.HallRepository;

import java.util.List;
import java.util.Locale;

public class HallService {
    private static final int MAX_CAPACITY = 200;

    private final HallRepository halls;
    private final BookingRepository bookings;

    public HallService(HallRepository halls, BookingRepository bookings) {
        this.halls = halls;
        this.bookings = bookings;
    }

    public Hall create(Hall hall) {
        validate(hall);
        return halls.create(hall);
    }

    public List<Hall> getAll() {
        return halls.findAll();
    }

    public Hall getById(long id) {
        return halls.findById(id).orElseThrow(() -> new EntityNotFoundException("Зал", id));
    }

    public Hall update(Hall hall) {
        getById(hall.getId());
        validate(hall);
        halls.update(hall);
        return hall;
    }

    public void delete(long id) {
        getById(id);
        if (bookings.existsByHallId(id)) {
            throw new BusinessException("Нельзя удалить зал, в котором есть бронирования");
        }
        halls.deleteById(id);
    }

    public List<Hall> search(String query) {
        String q = ClientService.normalizeQuery(query);
        return halls.findAll().stream()
                .filter(h -> h.getName().toLowerCase(Locale.ROOT).contains(q))
                .toList();
    }

    private void validate(Hall h) {
        if (h.getName() == null || h.getName().isBlank()) {
            throw new ValidationException("Название зала не может быть пустым");
        }
        if (h.getName().trim().length() > 100) {
            throw new ValidationException("Название зала не должно превышать 100 символов");
        }
        h.setName(h.getName().trim());
        if (h.getCapacity() < 1 || h.getCapacity() > MAX_CAPACITY) {
            throw new ValidationException("Вместимость зала должна быть от 1 до " + MAX_CAPACITY);
        }
        if (h.getHourlyPrice() == null || h.getHourlyPrice().signum() < 0) {
            throw new ValidationException("Цена за час не может быть отрицательной");
        }
        if (h.getDescription() != null && h.getDescription().length() > 255) {
            throw new ValidationException("Описание не должно превышать 255 символов");
        }
        for (Hall other : halls.findAll()) {
            if (other.getId() != h.getId() && other.getName().equalsIgnoreCase(h.getName())) {
                throw new BusinessException("Зал с таким названием уже существует");
            }
        }
    }
}
