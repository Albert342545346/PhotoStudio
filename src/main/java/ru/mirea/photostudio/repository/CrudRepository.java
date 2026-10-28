package ru.mirea.photostudio.repository;

import ru.mirea.photostudio.model.Identifiable;

import java.util.List;
import java.util.Optional;

/** Базовый интерфейс репозитория: создание, чтение, изменение, удаление. */
public interface CrudRepository<T extends Identifiable> {
    T create(T entity);

    Optional<T> findById(long id);

    List<T> findAll();

    void update(T entity);

    boolean deleteById(long id);
}
