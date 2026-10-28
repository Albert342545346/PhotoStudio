package ru.mirea.photostudio.model;

import java.math.BigDecimal;

/** Фотограф студии. */
public class Photographer extends Person {
    private SessionType specialization;
    private BigDecimal hourlyRate;
    private boolean active;

    public Photographer(long id, String fullName, String phone, String email,
                        SessionType specialization, BigDecimal hourlyRate, boolean active) {
        super(id, fullName, phone, email);
        this.specialization = specialization;
        this.hourlyRate = hourlyRate;
        this.active = active;
    }

    public Photographer(String fullName, String phone, String email,
                        SessionType specialization, BigDecimal hourlyRate, boolean active) {
        this(0, fullName, phone, email, specialization, hourlyRate, active);
    }

    public SessionType getSpecialization() {
        return specialization;
    }

    public void setSpecialization(SessionType specialization) {
        this.specialization = specialization;
    }

    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }

    public void setHourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String getRoleTitle() {
        return active ? "Фотограф" : "Фотограф (не работает)";
    }
}
