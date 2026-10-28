package ru.mirea.photostudio.model;

/** Поле, по которому сортируется список бронирований (КР 2, графический интерфейс). */
public enum BookingSortField implements Titled {
    START_TIME("Дате начала"),
    PRICE("Стоимости"),
    CREATED_AT("Дате создания"),
    CLIENT_NAME("Имени клиента");

    private final String title;

    BookingSortField(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
