package ru.mirea.photostudio.model;

/** Абстрактный человек: общая часть клиента и фотографа (наследование + полиморфизм). */
public abstract class Person implements Identifiable {
    private long id;
    private String fullName;
    private String phone;
    private String email;

    protected Person(long id, String fullName, String phone, String email) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
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

    /** Роль человека в системе — у каждого наследника своя. */
    public abstract String getRoleTitle();

    @Override
    public String toString() {
        return getRoleTitle() + " #" + id + " " + fullName;
    }
}
