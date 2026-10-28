package ru.mirea.photostudio.service;

import ru.mirea.photostudio.model.BookingSortField;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;

import java.time.LocalDate;

/**
 * Параметры выборки бронирований: поиск по тексту, фильтры (любое поле может быть null — «не применять»)
 * и сортировка. Все условия объединяются через «И».
 */
public record BookingQuery(String text, BookingStatus status, SessionType sessionType,
                           LocalDate from, LocalDate to, BookingSortField sortField, boolean ascending) {

    public static BookingQuery all() {
        return new BookingQuery(null, null, null, null, null, BookingSortField.START_TIME, false);
    }
}
