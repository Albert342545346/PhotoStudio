package ru.mirea.photostudio.exception;

/** Ошибка экспорта данных в файл. */
public class ExportException extends PhotostudioException {
    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
