package ru.mirea.photostudio.model;

import java.time.LocalDateTime;

/** Клиент фотостудии. VIP-клиенты получают скидку. */
public class Client extends Person {
    private boolean vip;
    private LocalDateTime registeredAt;

    public Client(long id, String fullName, String phone, String email, boolean vip, LocalDateTime registeredAt) {
        super(id, fullName, phone, email);
        this.vip = vip;
        this.registeredAt = registeredAt;
    }

    public Client(String fullName, String phone, String email, boolean vip) {
        this(0, fullName, phone, email, vip, LocalDateTime.now());
    }

    public boolean isVip() {
        return vip;
    }

    public void setVip(boolean vip) {
        this.vip = vip;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String getRoleTitle() {
        return vip ? "VIP-клиент" : "Клиент";
    }
}
