package ru.mirea.photostudio.exception;

/**
 * Базовое исключение приложения. Все собственные исключения наследуются от него,
 * поэтому в консольном меню их можно перехватить одним блоком catch (AppException e).
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
