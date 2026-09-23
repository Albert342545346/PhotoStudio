package ru.mirea.photostudio.exception;

/** Запись с указанным ID не найдена в базе данных. */
public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(String entityName, int id) {
        super(entityName + " с ID " + id + " не найден(а).");
    }
}
