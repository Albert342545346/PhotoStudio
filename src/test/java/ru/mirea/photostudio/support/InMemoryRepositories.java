package ru.mirea.photostudio.support;

import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.model.Identifiable;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.repository.PaymentRepository;
import ru.mirea.photostudio.repository.PhotographerRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Репозитории в памяти — для unit-тестов сервисов без базы данных. */
public final class InMemoryRepositories {
    private InMemoryRepositories() {
    }

    public static class Memory<T extends Identifiable> {
        protected final Map<Long, T> data = new LinkedHashMap<>();
        private long sequence = 0;

        public T create(T entity) {
            entity.setId(++sequence);
            data.put(entity.getId(), entity);
            return entity;
        }

        public Optional<T> findById(long id) {
            return Optional.ofNullable(data.get(id));
        }

        public List<T> findAll() {
            return new ArrayList<>(data.values());
        }

        public void update(T entity) {
            data.put(entity.getId(), entity);
        }

        public boolean deleteById(long id) {
            return data.remove(id) != null;
        }
    }

    public static class Clients extends Memory<Client> implements ClientRepository {
    }

    public static class Photographers extends Memory<Photographer> implements PhotographerRepository {
    }

    public static class Halls extends Memory<Hall> implements HallRepository {
    }

    public static class Payments extends Memory<Payment> implements PaymentRepository {
        @Override
        public List<Payment> findByBookingId(long bookingId) {
            return findAll().stream().filter(p -> p.getBookingId() == bookingId).toList();
        }

        @Override
        public boolean existsByBookingId(long bookingId) {
            return !findByBookingId(bookingId).isEmpty();
        }

        @Override
        public BigDecimal sumByBookingId(long bookingId) {
            return findByBookingId(bookingId).stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        @Override
        public BigDecimal sumAll() {
            return findAll().stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    public static class Bookings extends Memory<Booking> implements BookingRepository {
        @Override
        public List<Booking> findByDate(LocalDate date) {
            return findAll().stream().filter(b -> b.getStartTime().toLocalDate().equals(date)).toList();
        }

        private List<Booking> overlapping(Predicate<Booking> owner, LocalDateTime start, LocalDateTime end, long excludeId) {
            return findAll().stream().filter(owner)
                    .filter(b -> b.getStatus().isActive() && b.getId() != excludeId)
                    .filter(b -> b.getStartTime().isBefore(end) && b.getEndTime().isAfter(start))
                    .toList();
        }

        @Override
        public List<Booking> findActiveOverlappingForPhotographer(long id, LocalDateTime start, LocalDateTime end,
                                                                  long excludeId) {
            return overlapping(b -> b.getPhotographerId() == id, start, end, excludeId);
        }

        @Override
        public List<Booking> findActiveOverlappingForHall(long id, LocalDateTime start, LocalDateTime end,
                                                          long excludeId) {
            return overlapping(b -> b.getHallId() == id, start, end, excludeId);
        }

        @Override
        public boolean existsByClientId(long id) {
            return findAll().stream().anyMatch(b -> b.getClientId() == id);
        }

        @Override
        public boolean existsByPhotographerId(long id) {
            return findAll().stream().anyMatch(b -> b.getPhotographerId() == id);
        }

        @Override
        public boolean existsByHallId(long id) {
            return findAll().stream().anyMatch(b -> b.getHallId() == id);
        }
    }
}
