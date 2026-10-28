package ru.mirea.photostudio.util;

import ru.mirea.photostudio.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Управляет подключением к PostgreSQL. Настройки берутся из database.properties,
 * их можно переопределить переменными окружения DB_URL, DB_USER, DB_PASSWORD.
 */
public class DatabaseManager {
    private final String url;
    private final String user;
    private final String password;

    public DatabaseManager() {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getResourceAsStream("/database.properties")) {
            if (in != null) {
                props.load(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать database.properties", e);
        }
        this.url = env("DB_URL", props.getProperty("db.url", "jdbc:postgresql://localhost:5432/photostudio"));
        this.user = env("DB_USER", props.getProperty("db.user", "postgres"));
        this.password = env("DB_PASSWORD", props.getProperty("db.password", "postgres"));
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new DatabaseException("JDBC-драйвер PostgreSQL не найден в classpath", e);
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    /** Каждый вызов открывает новое соединение; закрывать его нужно через try-with-resources. */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка подключения к базе данных: " + e.getMessage(), e);
        }
    }

    /**
     * Если таблицы ещё не созданы — выполняет sql/schema.sql и sql/data.sql.
     * @return true, если инициализация была выполнена
     */
    public boolean initializeIfNeeded() {
        try (Connection connection = getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, "clients", new String[]{"TABLE"})) {
            if (tables.next()) {
                return false;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка проверки структуры базы данных: " + e.getMessage(), e);
        }
        runScript("/sql/schema.sql");
        runScript("/sql/data.sql");
        return true;
    }

    /** Выполняет SQL-скрипт из ресурсов: строки-комментарии пропускаются, команды разделяются точкой с запятой. */
    public void runScript(String resourcePath) {
        String script;
        try (InputStream in = DatabaseManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new DatabaseException("SQL-скрипт не найден: " + resourcePath);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать SQL-скрипт " + resourcePath, e);
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\\R")) {
            if (!line.trim().startsWith("--")) {
                cleaned.append(line).append('\n');
            }
        }
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                for (String command : cleaned.toString().split(";")) {
                    if (!command.isBlank()) {
                        statement.execute(command);
                    }
                }
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw new DatabaseException("Ошибка выполнения скрипта " + resourcePath + ": " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка работы с базой данных: " + e.getMessage(), e);
        }
    }
}
