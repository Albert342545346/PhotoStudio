package ru.mirea.photostudio.repository;

import ru.mirea.photostudio.model.Booking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends CrudRepository<Booking> {
    /** Бронирования, начинающиеся в указанную дату. */
    List<Booking> findByDate(LocalDate date);

    /** Активные бронирования фотографа, пересекающиеся с интервалом (кроме excludeId). */
    List<Booking> findActiveOverlappingForPhotographer(long photographerId, LocalDateTime start,
                                                       LocalDateTime end, long excludeId);

    /** Активные бронирования зала, пересекающиеся с интервалом (кроме excludeId). */
    List<Booking> findActiveOverlappingForHall(long hallId, LocalDateTime start,
                                               LocalDateTime end, long excludeId);

    boolean existsByClientId(long clientId);

    boolean existsByPhotographerId(long photographerId);

    boolean existsByHallId(long hallId);
}
