package ru.mirea.photostudio.repository;

/**
 * Интерфейс полного набора операций CRUD: Create (save), Read (findById/findAll),
 * Update (update), Delete (deleteById). Его реализуют репозитории клиентов и бронирований.
 */
public interface CrudRepository<T> extends ReadRepository<T> {

    /** Сохраняет новую запись и записывает в объект присвоенный базой данных ID. */
    T save(T entity);

    void update(T entity);

    /** @return true, если запись была удалена */
    boolean deleteById(int id);
}
