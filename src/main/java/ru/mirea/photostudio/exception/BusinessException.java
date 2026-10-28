package ru.mirea.photostudio.exception;

/** Нарушено бизнес-правило предметной области. */
public class BusinessException extends PhotostudioException {
    public BusinessException(String message) {
        super(message);
    }
}
