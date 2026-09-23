package ru.mirea.photostudio.repository;

import ru.mirea.photostudio.exception.DatabaseException;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Справочник залов: только чтение (интерфейс ReadRepository). */
public class HallRepository implements ReadRepository<Hall> {

    private static final String SELECT =
            "SELECT id, name, description, capacity, price_per_hour FROM halls";

    @Override
    public Optional<Hall> findById(int id) {
        String sql = SELECT + " WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при поиске зала", e);
        }
    }

    @Override
    public List<Hall> findAll() {
        List<Hall> halls = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(SELECT + " ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                halls.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при чтении залов", e);
        }
        return halls;
    }

    private Hall mapRow(ResultSet rs) throws SQLException {
        return new Hall(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getInt("capacity"),
                rs.getBigDecimal("price_per_hour"));
    }
}
