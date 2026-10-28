package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.exception.ValidationException;
import ru.mirea.photostudio.model.Titled;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Scanner;

/**
 * Безопасный консольный ввод: любая ошибка формата превращается в ValidationException,
 * которая перехватывается в меню — программа не завершается аварийно.
 */
public class ConsoleInput {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;

    public ConsoleInput(InputStream in) {
        this.scanner = new Scanner(in, StandardCharsets.UTF_8);
    }

    /** Читает строку. При закрытии ввода бросает NoSuchElementException (обрабатывается в Main). */
    public String line(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    public long id(String prompt) {
        String text = line(prompt);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ValidationException("ID должен быть целым числом.");
        }
    }

    public Long optionalId(String prompt) {
        String text = line(prompt);
        if (text.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new ValidationException("ID должен быть целым числом.");
        }
    }

    public int integer(String prompt, String field) {
        return parseInt(line(prompt), field);
    }

    public Integer optionalInteger(String prompt, String field) {
        String text = line(prompt);
        return text.isEmpty() ? null : parseInt(text, field);
    }

    public BigDecimal decimal(String prompt, String field) {
        return parseDecimal(line(prompt), field);
    }

    public BigDecimal optionalDecimal(String prompt, String field) {
        String text = line(prompt);
        return text.isEmpty() ? null : parseDecimal(text, field);
    }

    public LocalDateTime dateTime(String prompt) {
        return parseDateTime(line(prompt));
    }

    public LocalDateTime optionalDateTime(String prompt) {
        String text = line(prompt);
        return text.isEmpty() ? null : parseDateTime(text);
    }

    public LocalDate date(String prompt) {
        String text = line(prompt);
        try {
            return LocalDate.parse(text, DATE);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Неверный формат даты. Используйте дд.мм.гггг, например 15.11.2026.");
        }
    }

    public boolean yesNo(String prompt) {
        String text = line(prompt + " (да/нет): ").toLowerCase();
        return switch (text) {
            case "да", "д", "yes", "y", "1" -> true;
            case "нет", "н", "no", "n", "0" -> false;
            default -> throw new ValidationException("Ответьте «да» или «нет».");
        };
    }

    /** null означает «оставить без изменений» (пустой ввод). */
    public Boolean optionalYesNo(String prompt) {
        String text = line(prompt + " (да/нет, Enter — без изменений): ").toLowerCase();
        if (text.isEmpty()) {
            return null;
        }
        return switch (text) {
            case "да", "д", "yes", "y", "1" -> true;
            case "нет", "н", "no", "n", "0" -> false;
            default -> throw new ValidationException("Ответьте «да» или «нет».");
        };
    }

    public <E extends Enum<E> & Titled> E choose(String prompt, E[] values) {
        printOptions(prompt, values);
        return parseChoice(line("Ваш выбор: "), values);
    }

    /** Возвращает null при пустом вводе (оставить без изменений). */
    public <E extends Enum<E> & Titled> E chooseOptional(String prompt, E[] values) {
        printOptions(prompt + " (Enter — без изменений)", values);
        String text = line("Ваш выбор: ");
        return text.isEmpty() ? null : parseChoice(text, values);
    }

    private <E extends Enum<E> & Titled> void printOptions(String prompt, E[] values) {
        System.out.println(prompt);
        for (int i = 0; i < values.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, values[i].getTitle());
        }
    }

    private <E extends Enum<E> & Titled> E parseChoice(String text, E[] values) {
        int number = parseInt(text, "Номер варианта");
        if (number < 1 || number > values.length) {
            throw new ValidationException("Нет варианта с номером " + number + ".");
        }
        return values[number - 1];
    }

    private int parseInt(String text, String field) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " должно быть целым числом.");
        }
    }

    private BigDecimal parseDecimal(String text, String field) {
        try {
            return new BigDecimal(text.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " должно быть числом (например, 1500 или 1500.50).");
        }
    }

    private LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text, DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Неверный формат даты и времени. Используйте дд.мм.гггг чч:мм, "
                    + "например 15.11.2026 12:00.");
        }
    }
}
