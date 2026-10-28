package ru.mirea.photostudio.fx;

import javafx.application.Application;

/**
 * Запускающий класс JavaFX-приложения. Он нужен, потому что главный класс, унаследованный от Application,
 * нельзя запускать напрямую из jar/IDE без модулей JavaFX.
 */
public final class Launcher {
    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(PhotostudioFxApplication.class, args);
    }
}
