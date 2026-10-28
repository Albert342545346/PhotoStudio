package ru.mirea.photostudio.repository.jdbc;

import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.repository.PhotographerRepository;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JdbcPhotographerRepository extends AbstractJdbcRepository<Photographer> implements PhotographerRepository {

    public JdbcPhotographerRepository(DatabaseManager db) {
        super(db);
    }

    @Override
    protected String table() {
        return "photographers";
    }

    @Override
    protected String entityName() {
        return "Фотограф";
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO photographers (full_name, phone, email, specialization, hourly_rate, active) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void bindInsert(PreparedStatement st, Photographer p) throws SQLException {
        st.setString(1, p.getFullName());
        st.setString(2, p.getPhone());
        st.setString(3, p.getEmail());
        st.setString(4, p.getSpecialization().name());
        st.setBigDecimal(5, p.getHourlyRate());
        st.setBoolean(6, p.isActive());
    }

    @Override
    protected String updateSql() {
        return "UPDATE photographers SET full_name = ?, phone = ?, email = ?, specialization = ?, "
                + "hourly_rate = ?, active = ? WHERE id = ?";
    }

    @Override
    protected void bindUpdate(PreparedStatement st, Photographer p) throws SQLException {
        bindInsert(st, p);
        st.setLong(7, p.getId());
    }

    @Override
    protected Photographer map(ResultSet rs) throws SQLException {
        return new Photographer(rs.getLong("id"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("email"), SessionType.valueOf(rs.getString("specialization")),
                rs.getBigDecimal("hourly_rate"), rs.getBoolean("active"));
    }
}
