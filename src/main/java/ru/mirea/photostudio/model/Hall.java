package ru.mirea.photostudio.model;

import java.math.BigDecimal;

/** Зал (студия) для фотосъёмки. */
public class Hall extends BaseEntity {

    private final String name;
    private final String description;
    private final int capacity;
    private final BigDecimal pricePerHour;

    public Hall(int id, String name, String description, int capacity, BigDecimal pricePerHour) {
        super(id);
        this.name = name;
        this.description = description;
        this.capacity = capacity;
        this.pricePerHour = pricePerHour;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getPricePerHour() {
        return pricePerHour;
    }

    @Override
    public String describe() {
        return String.format("#%-2d %-18s вместимость: %-3d %8.2f ₽/час  — %s",
                getId(), name, capacity, pricePerHour, description == null ? "" : description);
    }
}
