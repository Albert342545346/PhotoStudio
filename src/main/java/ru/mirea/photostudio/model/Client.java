package ru.mirea.photostudio.model;

import java.time.LocalDateTime;

/** Клиент фотостудии (пользователь системы). */
public class Client extends BaseEntity {

    private String fullName;
    private String phone;
    private String email;
    private LocalDateTime registeredAt;

    /** Конструктор для нового клиента (ID и дату регистрации назначит база данных). */
    public Client(String fullName, String phone, String email) {
        this(0, fullName, phone, email, null);
    }

    /** Конструктор для клиента, прочитанного из базы данных. */
    public Client(int id, String fullName, String phone, String email, LocalDateTime registeredAt) {
        super(id);
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.registeredAt = registeredAt;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    @Override
    public String describe() {
        return String.format("#%-3d %-32s %-15s %s", getId(), fullName, phone, email);
    }
}
