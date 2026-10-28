package ru.mirea.photostudio.exception;

/** Базовое исключение приложения. Все собственные исключения наследуются от него. */
public class PhotostudioException extends RuntimeException {
    public PhotostudioException(String message) {
        super(message);
    }

    public PhotostudioException(String message, Throwable cause) {
        super(message, cause);
    }
}
