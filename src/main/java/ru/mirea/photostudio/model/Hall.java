package ru.mirea.photostudio.model;

import java.math.BigDecimal;

/** Зал (фотолокация) студии. */
public class Hall implements Identifiable {
    private long id;
    private String name;
    private int capacity;
    private BigDecimal hourlyPrice;
    private String description;

    public Hall(long id, String name, int capacity, BigDecimal hourlyPrice, String description) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.hourlyPrice = hourlyPrice;
        this.description = description;
    }

    public Hall(String name, int capacity, BigDecimal hourlyPrice, String description) {
        this(0, name, capacity, hourlyPrice, description);
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getHourlyPrice() {
        return hourlyPrice;
    }

    public void setHourlyPrice(BigDecimal hourlyPrice) {
        this.hourlyPrice = hourlyPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Зал #" + id + " " + name;
    }
}
