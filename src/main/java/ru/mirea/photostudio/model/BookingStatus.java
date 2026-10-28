package ru.mirea.photostudio.model;

import java.util.Arrays;
import java.util.List;

/** Статус бронирования и разрешённые переходы между статусами. */
public enum BookingStatus implements Titled {
    CREATED("Создано"),
    CONFIRMED("Подтверждено"),
    COMPLETED("Завершено"),
    CANCELLED("Отменено");

    private final String title;

    BookingStatus(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    /** Бронирование занимает время фотографа и зала. */
    public boolean isActive() {
        return this == CREATED || this == CONFIRMED;
    }

    /** Из конечного статуса выйти нельзя. */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * CREATED   -> CONFIRMED | CANCELLED
     * CONFIRMED -> COMPLETED | CANCELLED
     * COMPLETED, CANCELLED -> (переходов нет)
     */
    public boolean canTransitionTo(BookingStatus next) {
        return switch (this) {
            case CREATED -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == COMPLETED || next == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /** Статусы, в которые можно перейти из текущего. */
    public List<BookingStatus> nextStatuses() {
        return Arrays.stream(values()).filter(this::canTransitionTo).toList();
    }
}
