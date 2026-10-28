package ru.mirea.photostudio.exception;

/** Ошибка подключения к базе данных или выполнения SQL-запроса. */
public class DatabaseException extends PhotostudioException {
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseException(String message) {
        super(message);
    }
}
