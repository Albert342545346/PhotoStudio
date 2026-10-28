package ru.mirea.photostudio.repository.jdbc;

import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.PaymentMethod;
import ru.mirea.photostudio.repository.PaymentRepository;
import ru.mirea.photostudio.util.DatabaseManager;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class JdbcPaymentRepository extends AbstractJdbcRepository<Payment> implements PaymentRepository {

    public JdbcPaymentRepository(DatabaseManager db) {
        super(db);
    }

    @Override
    protected String table() {
        return "payments";
    }

    @Override
    protected String entityName() {
        return "Платёж";
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO payments (booking_id, amount, method, paid_at) VALUES (?, ?, ?, ?)";
    }

    @Override
    protected void bindInsert(PreparedStatement st, Payment p) throws SQLException {
        st.setLong(1, p.getBookingId());
        st.setBigDecimal(2, p.getAmount());
        st.setString(3, p.getMethod().name());
        st.setTimestamp(4, Timestamp.valueOf(p.getPaidAt()));
    }

    @Override
    protected String updateSql() {
        return "UPDATE payments SET booking_id = ?, amount = ?, method = ?, paid_at = ? WHERE id = ?";
    }

    @Override
    protected void bindUpdate(PreparedStatement st, Payment p) throws SQLException {
        bindInsert(st, p);
        st.setLong(5, p.getId());
    }

    @Override
    protected Payment map(ResultSet rs) throws SQLException {
        return new Payment(rs.getLong("id"), rs.getLong("booking_id"), rs.getBigDecimal("amount"),
                PaymentMethod.valueOf(rs.getString("method")), rs.getTimestamp("paid_at").toLocalDateTime());
    }

    @Override
    public List<Payment> findByBookingId(long bookingId) {
        return query("SELECT * FROM payments WHERE booking_id = ? ORDER BY paid_at", st -> st.setLong(1, bookingId));
    }

    @Override
    public boolean existsByBookingId(long bookingId) {
        return exists("SELECT 1 FROM payments WHERE booking_id = ? LIMIT 1", st -> st.setLong(1, bookingId));
    }

    @Override
    public BigDecimal sumByBookingId(long bookingId) {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE booking_id = ?")) {
            statement.setLong(1, bookingId);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    @Override
    public BigDecimal sumAll() {
        try (Connection connection = db.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM payments");
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
        } catch (SQLException e) {
            throw translate(e);
        }
    }
}
