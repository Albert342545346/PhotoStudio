package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.exception.InvalidInputException;
import ru.mirea.photostudio.model.Titled;
import ru.mirea.photostudio.util.Formats;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Безопасное чтение данных с консоли. При неверном формате бросает InvalidInputException
 * (её ловит меню и показывает понятное сообщение) — программа не падает.
 */
public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(InputStream in) {
        this.scanner = new Scanner(in, StandardCharsets.UTF_8);
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) { // ввод закрыт (Ctrl+D / Ctrl+Z) — завершаем программу штатно
            System.out.println();
            System.out.println("Ввод завершён. Выход из программы.");
            System.exit(0);
        }
        return scanner.nextLine();
    }

    /** Строка; если пользователь нажал Enter — возвращается текущее значение (режим редактирования). */
    public String readLine(String prompt, String current) {
        String value = readLine(prompt + " [" + current + "]: ");
        return value.isBlank() ? current : value.trim();
    }

    public int readInt(String prompt, String errorMessage) {
        String value = readLine(prompt).trim();
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(errorMessage);
        }
    }

    /** Целое число; Enter — оставить текущее значение. */
    public int readInt(String prompt, String errorMessage, int current) {
        String value = readLine(prompt + " [" + current + "]: ").trim();
        if (value.isEmpty()) {
            return current;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new InvalidInputException(errorMessage);
        }
    }

    public int readId(String prompt) {
        return readInt(prompt, "ID должен быть целым числом.");
    }

    public LocalDate readDate(String prompt) {
        String value = readLine(prompt + " (дд.мм.гггг): ").trim();
        try {
            return LocalDate.parse(value, Formats.DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Неверная дата. Используйте формат дд.мм.гггг, например 25.09.2026.");
        }
    }

    /** Дата и время; если current не null, Enter оставляет текущее значение. */
    public LocalDateTime readDateTime(String prompt, LocalDateTime current) {
        String hint = current == null ? " (дд.мм.гггг чч:мм)" : " [" + current.format(Formats.DATE_TIME) + "]";
        String value = readLine(prompt + hint + ": ").trim();
        if (value.isEmpty() && current != null) {
            return current;
        }
        try {
            return LocalDateTime.parse(value, Formats.DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException(
                    "Неверная дата/время. Используйте формат дд.мм.гггг чч:мм, например 25.09.2026 14:00.");
        }
    }

    /**
     * Универсальный выбор значения enum из нумерованного списка.
     * Работает с любым enum, который реализует интерфейс Titled (ограничение generics).
     * Если current не null, Enter оставляет текущее значение.
     */
    public <E extends Enum<E> & Titled> E readEnum(String prompt, E[] values, E current) {
        System.out.println(prompt + ":");
        for (int i = 0; i < values.length; i++) {
            System.out.println("  " + (i + 1) + ". " + values[i].getTitle());
        }
        String hint = current == null ? "Ваш выбор: " : "Ваш выбор [" + current.getTitle() + "]: ";
        String value = readLine(hint).trim();
        if (value.isEmpty() && current != null) {
            return current;
        }
        try {
            int index = Integer.parseInt(value);
            if (index >= 1 && index <= values.length) {
                return values[index - 1];
            }
        } catch (NumberFormatException ignored) {
            // упадём в throw ниже
        }
        throw new InvalidInputException("Нужно ввести номер из списка (от 1 до " + values.length + ").");
    }

    public boolean confirm(String question) {
        String answer = readLine(question + " (да/нет): ").trim().toLowerCase();
        return answer.equals("да") || answer.equals("д") || answer.equals("y") || answer.equals("yes");
    }
}
