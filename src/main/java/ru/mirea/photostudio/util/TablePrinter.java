package ru.mirea.photostudio.util;

import ru.mirea.photostudio.exception.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Формирует текстовый вывод содержимого таблиц БД (пункт меню «Вывести таблицы базы данных»). */
public class TablePrinter {
    /** Белый список: имя таблицы подставляется в SQL только из этого списка. */
    public static final List<String> TABLES = List.of("clients", "photographers", "halls", "bookings", "payments");
    private static final int MAX_WIDTH = 28;

    private final DatabaseManager db;

    public TablePrinter(DatabaseManager db) {
        this.db = db;
    }

    public String dump(String table) {
        if (!TABLES.contains(table)) {
            throw new DatabaseException("Неизвестная таблица: " + table);
        }
        String sql = "SELECT * FROM " + table + " ORDER BY id";
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columns = meta.getColumnCount();
            List<String[]> rows = new ArrayList<>();
            String[] header = new String[columns];
            for (int i = 0; i < columns; i++) {
                header[i] = meta.getColumnLabel(i + 1);
            }
            rows.add(header);
            while (rs.next()) {
                String[] row = new String[columns];
                for (int i = 0; i < columns; i++) {
                    Object value = rs.getObject(i + 1);
                    row[i] = value == null ? "NULL" : String.valueOf(value);
                }
                rows.add(row);
            }
            return format(table, rows, columns);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL-запроса: " + e.getMessage(), e);
        }
    }

    private String format(String table, List<String[]> rows, int columns) {
        int[] widths = new int[columns];
        for (String[] row : rows) {
            for (int i = 0; i < columns; i++) {
                widths[i] = Math.max(widths[i], Math.min(row[i].length(), MAX_WIDTH));
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("--- Таблица ").append(table).append(" (записей: ").append(rows.size() - 1).append(") ---\n");
        for (int r = 0; r < rows.size(); r++) {
            String[] row = rows.get(r);
            for (int i = 0; i < columns; i++) {
                String cell = row[i].length() > MAX_WIDTH ? row[i].substring(0, MAX_WIDTH - 1) + "…" : row[i];
                sb.append(String.format("%-" + widths[i] + "s", cell));
                sb.append(i < columns - 1 ? " | " : "\n");
            }
            if (r == 0) {
                for (int i = 0; i < columns; i++) {
                    sb.append("-".repeat(widths[i])).append(i < columns - 1 ? "-+-" : "\n");
                }
            }
        }
        return sb.toString();
    }
}
