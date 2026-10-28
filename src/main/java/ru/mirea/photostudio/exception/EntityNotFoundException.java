package ru.mirea.photostudio.exception;

/** Запись с указанным ID не найдена. */
public class EntityNotFoundException extends PhotostudioException {
    public EntityNotFoundException(String entityName, long id) {
        super(entityName + " с ID " + id + " не найден(а)");
    }
}
