package ru.mirea.photostudio.repository;

import java.util.List;
import java.util.Optional;

/** Интерфейс только для чтения данных (используется, например, для справочника залов). */
public interface ReadRepository<T> {

    Optional<T> findById(int id);

    List<T> findAll();
}
