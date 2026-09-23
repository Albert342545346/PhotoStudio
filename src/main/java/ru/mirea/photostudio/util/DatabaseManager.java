package ru.mirea.photostudio.util;

import ru.mirea.photostudio.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Единственное место, где создаётся подключение к базе данных.
 * Настройки читаются так (по приоритету):
 *   1) переменные окружения DB_URL, DB_USER, DB_PASSWORD;
 *   2) файл db.properties рядом с программой (в рабочей папке);
 *   3) файл db.properties внутри проекта (src/main/resources).
 */
public final class DatabaseManager {

    private static Properties properties;

    private DatabaseManager() {
    }

    public static Connection getConnection() {
        Properties p = loadProperties();
        try {
            return DriverManager.getConnection(
                    p.getProperty("db.url"), p.getProperty("db.user"), p.getProperty("db.password"));
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подключения к базе данных", e);
        }
    }

    /** Проверка подключения при запуске программы. Бросает DatabaseException, если БД недоступна. */
    public static void checkConnection() {
        try (Connection ignored = getConnection()) {
            // подключение открылось и сразу закрылось (try-with-resources)
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подключения к базе данных", e);
        }
    }

    private static synchronized Properties loadProperties() {
        if (properties != null) {
            return properties;
        }
        Properties p = new Properties();
        Path external = Path.of("db.properties");
        try {
            if (Files.exists(external)) {
                try (Reader reader = Files.newBufferedReader(external, StandardCharsets.UTF_8)) {
                    p.load(reader);
                }
            } else {
                try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream("db.properties")) {
                    if (in == null) {
                        throw new DatabaseException("Файл db.properties не найден");
                    }
                    p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать db.properties: " + e.getMessage());
        }
        override(p, "db.url", "DB_URL");
        override(p, "db.user", "DB_USER");
        override(p, "db.password", "DB_PASSWORD");
        properties = p;
        return p;
    }

    private static void override(Properties p, String key, String envName) {
        String value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            p.setProperty(key, value);
        }
    }
}
