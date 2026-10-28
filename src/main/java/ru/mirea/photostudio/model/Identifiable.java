package ru.mirea.photostudio.model;

/** Любая сущность, хранящаяся в БД и имеющая числовой идентификатор. */
public interface Identifiable {
    long getId();

    void setId(long id);
}
