package ru.mirea.photostudio.repository.jdbc;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.DatabaseException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Identifiable;
import ru.mirea.photostudio.repository.CrudRepository;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Общая JDBC-логика: Connection + PreparedStatement + ResultSet в try-with-resources.
 * Наследники задают только SQL и отображение строки на объект.
 * Имя таблицы — константа наследника, не пользовательский ввод; все значения передаются через «?».
 */
public abstract class AbstractJdbcRepository<T extends Identifiable> implements CrudRepository<T> {
    protected final DatabaseManager db;

    protected AbstractJdbcRepository(DatabaseManager db) {
        this.db = db;
    }

    /** Подготовка параметров PreparedStatement. */
    @FunctionalInterface
    protected interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    protected abstract String table();

    protected abstract String entityName();

    protected abstract String insertSql();

    protected abstract void bindInsert(PreparedStatement statement, T entity) throws SQLException;

    /** UPDATE ... WHERE id = ?  (id — последний параметр). */
    protected abstract String updateSql();

    protected abstract void bindUpdate(PreparedStatement statement, T entity) throws SQLException;

    protected abstract T map(ResultSet rs) throws SQLException;

    @Override
    public T create(T entity) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(insertSql(), new String[]{"id"})) {
            bindInsert(statement, entity);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    entity.setId(keys.getLong(1));
                }
            }
            return entity;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public Optional<T> findById(long id) {
        List<T> result = query("SELECT * FROM " + table() + " WHERE id = ?", st -> st.setLong(1, id));
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }

    @Override
    public List<T> findAll() {
        return query("SELECT * FROM " + table() + " ORDER BY id", st -> { });
    }

    @Override
    public void update(T entity) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(updateSql())) {
            bindUpdate(statement, entity);
            if (statement.executeUpdate() == 0) {
                throw new EntityNotFoundException(entityName(), entity.getId());
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public boolean deleteById(long id) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM " + table() + " WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    protected List<T> query(String sql, Binder binder) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                List<T> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    protected boolean exists(String sql, Binder binder) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Преобразует SQLException в понятное приложению исключение. */
    protected RuntimeException translate(SQLException e) {
        String state = e.getSQLState();
        if ("23505".equals(state)) {
            return new BusinessException("Запись с такими уникальными данными (телефон, email или название) уже существует");
        }
        if ("23503".equals(state)) {
            return new BusinessException("Операция нарушает связь между таблицами (запись используется в другой таблице)");
        }
        if ("23502".equals(state) || "23514".equals(state)) {
            return new BusinessException("Данные нарушают ограничения базы данных (NOT NULL / CHECK)");
        }
        return new DatabaseException("Ошибка выполнения SQL-запроса: " + e.getMessage(), e);
    }
}
