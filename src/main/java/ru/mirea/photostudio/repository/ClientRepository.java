package ru.mirea.photostudio.repository;

import ru.mirea.photostudio.exception.DatabaseException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Работа с таблицей clients через JDBC. Здесь находится только SQL, бизнес-логики нет. */
public class ClientRepository implements CrudRepository<Client> {

    private static final String SELECT =
            "SELECT id, full_name, phone, email, registered_at FROM clients";

    @Override
    public Client save(Client client) {
        String sql = "INSERT INTO clients (full_name, phone, email) VALUES (?, ?, ?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"})) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    client.setId(keys.getInt(1));
                }
            }
            return client;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при добавлении клиента", e);
        }
    }

    @Override
    public Optional<Client> findById(int id) {
        String sql = SELECT + " WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при поиске клиента", e);
        }
    }

    @Override
    public List<Client> findAll() {
        return query(SELECT + " ORDER BY id", null);
    }

    @Override
    public void update(Client client) {
        String sql = "UPDATE clients SET full_name = ?, phone = ?, email = ? WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, client.getFullName());
            ps.setString(2, client.getPhone());
            ps.setString(3, client.getEmail());
            ps.setInt(4, client.getId());
            if (ps.executeUpdate() == 0) {
                throw new EntityNotFoundException("Клиент", client.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при изменении клиента", e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при удалении клиента", e);
        }
    }

    public Optional<Client> findByEmail(String email) {
        String sql = SELECT + " WHERE LOWER(email) = LOWER(?)";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при поиске клиента по email", e);
        }
    }

    /** Поиск клиентов по части имени (без учёта регистра). */
    public List<Client> searchByName(String text) {
        return query(SELECT + " WHERE LOWER(full_name) LIKE LOWER(?) ORDER BY id", "%" + text + "%");
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM clients";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при подсчёте клиентов", e);
        }
    }

    /** Общий метод выполнения SELECT с одним необязательным строковым параметром. */
    private List<Client> query(String sql, String param) {
        List<Client> result = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при чтении клиентов", e);
        }
        return result;
    }

    private Client mapRow(ResultSet rs) throws SQLException {
        Timestamp registered = rs.getTimestamp("registered_at");
        return new Client(
                rs.getInt("id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                registered == null ? null : registered.toLocalDateTime());
    }
}
