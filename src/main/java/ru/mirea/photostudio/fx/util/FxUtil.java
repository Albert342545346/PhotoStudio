package ru.mirea.photostudio.fx.util;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.photostudio.exception.PhotostudioException;
import ru.mirea.photostudio.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Вспомогательные методы для форм и таблиц JavaFX: форматирование, проверка ввода, показ ошибок. */
public final class FxUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FxUtil.class);
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private FxUtil() {
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public static String money(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " ₽";
    }

    /**
     * Настраивает ComboBox: как показывать элементы в списке и в поле выбора.
     * Если ничего не выбрано, в поле показывается подсказка prompt.
     */
    public static <T> void configureCombo(ComboBox<T> combo, Function<T, String> label, String prompt) {
        combo.setPromptText(prompt);
        combo.setCellFactory(list -> new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : label.apply(item));
            }
        });
        combo.setButtonCell(new ListCell<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? prompt : label.apply(item));
            }
        });
    }

    // ---------- Проверка пользовательского ввода (сообщения называют поле и что исправить) ----------

    public static String requireText(String value, String field) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            throw new ValidationException("Поле «" + field + "» обязательно для заполнения");
        }
        return text;
    }

    public static String optionalText(String value) {
        return value == null ? "" : value.trim();
    }

    public static BigDecimal parseDecimal(String value, String field) {
        String text = requireText(value, field).replace(',', '.');
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new ValidationException("Поле «" + field + "» должно быть числом, например 1500 или 1500.50");
        }
    }

    public static int parseInt(String value, String field) {
        String text = requireText(value, field);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new ValidationException("Поле «" + field + "» должно быть целым числом");
        }
    }

    public static <T> T requireSelected(T value, String field) {
        if (value == null) {
            throw new ValidationException("Выберите значение в поле «" + field + "»");
        }
        return value;
    }

    // ---------- Ошибки ----------

    /** Текст для пользователя: бизнес-ошибки показываем как есть, технические — коротко, детали в журнал. */
    public static String messageOf(Throwable e) {
        if (e instanceof PhotostudioException) {
            return e.getMessage();
        }
        LOG.error("Непредвиденная ошибка", e);
        return "Непредвиденная ошибка. Подробности записаны в журнал приложения.";
    }

    public static void showFormError(Label label, Throwable e) {
        label.setText(messageOf(e));
        label.setVisible(true);
        label.setManaged(true);
    }

    public static void clearFormError(Label label) {
        label.setText("");
        label.setVisible(false);
        label.setManaged(false);
    }
}
