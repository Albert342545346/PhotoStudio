package ru.mirea.photostudio.service;

import ru.mirea.photostudio.model.Booking;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** Бронирование вместе с именами связанных объектов и суммой оплаты — для отображения в таблицах. */
public record BookingView(Booking booking, String clientName, String photographerName, String hallName,
                          BigDecimal paid) {

    public BigDecimal remaining() {
        return booking.getTotalPrice().subtract(paid);
    }

    /** Короткая подпись для списков выбора. */
    public String label() {
        return "№" + booking.getId() + " · " + clientName + " · "
                + DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(booking.getStartTime());
    }
}
