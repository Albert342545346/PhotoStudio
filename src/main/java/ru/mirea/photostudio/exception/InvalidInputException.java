package ru.mirea.photostudio.exception;

/** Пользователь ввёл данные неверного формата (текст вместо числа, неверная дата и т.п.). */
public class InvalidInputException extends AppException {

    public InvalidInputException(String message) {
        super(message);
    }
}
