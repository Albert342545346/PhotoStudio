package ru.mirea.photostudio.exception;

/** Нарушение бизнес-правила (например, зал занят, недопустимый переход статуса). */
public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(message);
    }
}
