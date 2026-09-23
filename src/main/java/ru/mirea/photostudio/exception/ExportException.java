package ru.mirea.photostudio.exception;

/** Ошибка при записи файла экспорта (Excel / CSV). */
public class ExportException extends AppException {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
