package ru.mirea.photostudio.model;

/**
 * Базовый класс для всех сущностей предметной области. Хранит идентификатор.
 * Метод describe() абстрактный: каждый наследник описывает себя по-своему (полиморфизм).
 */
public abstract class BaseEntity {

    private int id;

    protected BaseEntity() {
    }

    protected BaseEntity(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /** Текстовое представление для вывода в консоль. */
    public abstract String describe();

    @Override
    public String toString() {
        return describe();
    }
}
