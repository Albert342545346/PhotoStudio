package ru.mirea.photostudio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Общие форматы даты/времени и денег. STRICT не даёт ввести несуществующую дату, например 31.02.2026. */
public final class Formats {

    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("dd.MM.uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Formats() {
    }

    public static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString() + " ₽";
    }
}
