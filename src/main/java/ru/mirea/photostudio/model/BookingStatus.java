package ru.mirea.photostudio.model;

/**
 * Статус бронирования фотосессии.
 * Внутри enum хранится и правило допустимых переходов между статусами (бизнес-правило).
 */
public enum BookingStatus implements Titled {
    CREATED("Создана"),
    CONFIRMED("Подтверждена"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена");

    private final String title;

    BookingStatus(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    /** Допустимые переходы: CREATED -> CONFIRMED/CANCELLED, CONFIRMED -> COMPLETED/CANCELLED. */
    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case CREATED -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /** Конечный статус: после него бронь изменять нельзя. */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** Активная бронь: она занимает зал и ещё может состояться. */
    public boolean isActive() {
        return this == CREATED || this == CONFIRMED;
    }
}
