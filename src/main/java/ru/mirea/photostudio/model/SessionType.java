package ru.mirea.photostudio.model;

/** Тип фотосессии. */
public enum SessionType implements Titled {
    PORTRAIT("Портретная"),
    FAMILY("Семейная"),
    CHILDREN("Детская"),
    LOVE_STORY("Love story"),
    PRODUCT("Предметная"),
    CORPORATE("Корпоративная");

    private final String title;

    SessionType(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }
}
