package ru.mirea.photostudio.repository;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.DatabaseException;
import ru.mirea.photostudio.model.TableData;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Читает «как есть» содержимое таблиц БД (для пункта меню «Вывести таблицы базы данных»). */
public class DatabaseViewRepository {

    /**
     * Имя таблицы нельзя передать параметром «?» (параметры — только для значений),
     * поэтому берём его из белого списка. Пользовательский ввод сюда не попадает.
     */
    private static final List<String> TABLES = List.of("clients", "halls", "bookings");

    public List<String> getTableNames() {
        return TABLES;
    }

    public TableData readTable(String tableName) {
        if (!TABLES.contains(tableName)) {
            throw new BusinessException("Неизвестная таблица: " + tableName);
        }
        String sql = "SELECT * FROM " + tableName + " ORDER BY id";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }
            List<List<String>> rows = new ArrayList<>();
            while (rs.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    row.add(value == null ? "NULL" : value);
                }
                rows.add(row);
            }
            return new TableData(tableName, columns, rows);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при чтении таблицы " + tableName, e);
        }
    }
}
