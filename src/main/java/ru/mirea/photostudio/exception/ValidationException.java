package ru.mirea.photostudio.exception;

/** Некорректные данные: неверный формат ввода или недопустимое значение поля. */
public class ValidationException extends PhotostudioException {
    public ValidationException(String message) {
        super(message);
    }
}
