package ru.mirea.photostudio.model;

import java.math.BigDecimal;

/** Тип фотосъёмки. Коэффициент влияет на итоговую стоимость. */
public enum SessionType implements Titled {
    PORTRAIT("Портретная", "1.00"),
    FAMILY("Семейная", "1.10"),
    LOVE_STORY("Love story", "1.10"),
    PRODUCT("Предметная", "1.20"),
    CORPORATE("Корпоративная", "1.30"),
    WEDDING("Свадебная", "1.50");

    private final String title;
    private final BigDecimal coefficient;

    SessionType(String title, String coefficient) {
        this.title = title;
        this.coefficient = new BigDecimal(coefficient);
    }

    @Override
    public String getTitle() {
        return title;
    }

    public BigDecimal getCoefficient() {
        return coefficient;
    }
}
