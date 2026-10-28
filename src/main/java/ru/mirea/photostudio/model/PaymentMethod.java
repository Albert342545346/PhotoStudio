package ru.mirea.photostudio.model;

/** Способ оплаты. */
public enum PaymentMethod implements Titled {
    CARD("Банковская карта"),
    CASH("Наличные"),
    TRANSFER("Банковский перевод");

    private final String title;

    PaymentMethod(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
