package ru.mirea.photostudio.repository.jdbc;

import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class JdbcClientRepository extends AbstractJdbcRepository<Client> implements ClientRepository {

    public JdbcClientRepository(DatabaseManager db) {
        super(db);
    }

    @Override
    protected String table() {
        return "clients";
    }

    @Override
    protected String entityName() {
        return "Клиент";
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO clients (full_name, phone, email, vip, registered_at) VALUES (?, ?, ?, ?, ?)";
    }

    @Override
    protected void bindInsert(PreparedStatement st, Client c) throws SQLException {
        st.setString(1, c.getFullName());
        st.setString(2, c.getPhone());
        st.setString(3, c.getEmail());
        st.setBoolean(4, c.isVip());
        st.setTimestamp(5, Timestamp.valueOf(c.getRegisteredAt()));
    }

    @Override
    protected String updateSql() {
        return "UPDATE clients SET full_name = ?, phone = ?, email = ?, vip = ? WHERE id = ?";
    }

    @Override
    protected void bindUpdate(PreparedStatement st, Client c) throws SQLException {
        st.setString(1, c.getFullName());
        st.setString(2, c.getPhone());
        st.setString(3, c.getEmail());
        st.setBoolean(4, c.isVip());
        st.setLong(5, c.getId());
    }

    @Override
    protected Client map(ResultSet rs) throws SQLException {
        return new Client(rs.getLong("id"), rs.getString("full_name"), rs.getString("phone"),
                rs.getString("email"), rs.getBoolean("vip"), rs.getTimestamp("registered_at").toLocalDateTime());
    }
}
