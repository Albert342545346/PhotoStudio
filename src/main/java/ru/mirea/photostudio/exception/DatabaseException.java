package ru.mirea.photostudio.exception;

import java.sql.SQLException;

/** Ошибка подключения к БД или выполнения SQL-запроса (обёртка над SQLException). */
public class DatabaseException extends AppException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, SQLException cause) {
        super(message + ": " + cause.getMessage(), cause);
    }
}
