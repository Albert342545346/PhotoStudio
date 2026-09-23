package ru.mirea.photostudio.util;

import ru.mirea.photostudio.exception.ExportException;
import ru.mirea.photostudio.model.Booking;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Дополнительный экспорт бронирований в CSV (разделитель «;», кодировка UTF-8 с BOM — Excel откроет корректно). */
public class CsvExporter {

    private static final String SEPARATOR = ";";

    public Path exportBookings(List<Booking> bookings, Path target) {
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
                writer.write('\uFEFF'); // BOM
                writer.write(String.join(SEPARATOR, "ID", "Название", "Клиент", "Зал", "Тип съёмки",
                        "Начало", "Окончание", "Часов", "Стоимость", "Статус", "Описание"));
                writer.newLine();
                for (Booking b : bookings) {
                    writer.write(String.join(SEPARATOR,
                            String.valueOf(b.getId()),
                            escape(b.getTitle()),
                            escape(b.getClientName()),
                            escape(b.getHallName()),
                            escape(b.getSessionType().getTitle()),
                            b.getStartTime().format(Formats.DATE_TIME),
                            b.getEndTime().format(Formats.DATE_TIME),
                            String.valueOf(b.getDurationHours()),
                            b.getPrice().toPlainString(),
                            escape(b.getStatus().getTitle()),
                            escape(b.getDescription())));
                    writer.newLine();
                }
            }
            return target;
        } catch (IOException e) {
            throw new ExportException("Не удалось записать CSV-файл: " + e.getMessage(), e);
        }
    }

    /** Значения с «;», кавычками и переводами строк заключаются в кавычки, внутренние кавычки удваиваются. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
