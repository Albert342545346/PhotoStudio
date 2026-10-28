package ru.mirea.photostudio.repository.jdbc;

import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcHallRepository extends AbstractJdbcRepository<Hall> implements HallRepository {

    public JdbcHallRepository(DatabaseManager db) {
        super(db);
    }

    @Override
    protected String table() {
        return "halls";
    }

    @Override
    protected String entityName() {
        return "Зал";
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO halls (name, capacity, hourly_price, description) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void bindInsert(PreparedStatement st, Hall h) throws SQLException {
        st.setString(1, h.getName());
        st.setInt(2, h.getCapacity());
        st.setBigDecimal(3, h.getHourlyPrice());
        st.setString(4, h.getDescription());
    }

    @Override
    protected String updateSql() {
        return "UPDATE halls SET name = ?, capacity = ?, hourly_price = ?, description = ? WHERE id = ?";
    }

    @Override
    protected void bindUpdate(PreparedStatement st, Hall h) throws SQLException {
        bindInsert(st, h);
        st.setLong(5, h.getId());
    }

    @Override
    protected Hall map(ResultSet rs) throws SQLException {
        return new Hall(rs.getLong("id"), rs.getString("name"), rs.getInt("capacity"),
                rs.getBigDecimal("hourly_price"), rs.getString("description"));
    }
}
