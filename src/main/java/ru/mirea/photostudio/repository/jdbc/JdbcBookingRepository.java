package ru.mirea.photostudio.repository.jdbc;

import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcBookingRepository extends AbstractJdbcRepository<Booking> implements BookingRepository {

    private static final String OVERLAP_CONDITION =
            " AND status IN ('CREATED', 'CONFIRMED') AND start_time < ? AND end_time > ? AND id <> ?";

    public JdbcBookingRepository(DatabaseManager db) {
        super(db);
    }

    @Override
    protected String table() {
        return "bookings";
    }

    @Override
    protected String entityName() {
        return "Бронирование";
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO bookings (client_id, photographer_id, hall_id, session_type, status, start_time, "
                + "end_time, participants, total_price, comment, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected void bindInsert(PreparedStatement st, Booking b) throws SQLException {
        st.setLong(1, b.getClientId());
        st.setLong(2, b.getPhotographerId());
        st.setLong(3, b.getHallId());
        st.setString(4, b.getSessionType().name());
        st.setString(5, b.getStatus().name());
        st.setTimestamp(6, Timestamp.valueOf(b.getStartTime()));
        st.setTimestamp(7, Timestamp.valueOf(b.getEndTime()));
        st.setInt(8, b.getParticipants());
        st.setBigDecimal(9, b.getTotalPrice());
        st.setString(10, b.getComment());
        st.setTimestamp(11, Timestamp.valueOf(b.getCreatedAt()));
    }

    @Override
    protected String updateSql() {
        return "UPDATE bookings SET client_id = ?, photographer_id = ?, hall_id = ?, session_type = ?, status = ?, "
                + "start_time = ?, end_time = ?, participants = ?, total_price = ?, comment = ? WHERE id = ?";
    }

    @Override
    protected void bindUpdate(PreparedStatement st, Booking b) throws SQLException {
        st.setLong(1, b.getClientId());
        st.setLong(2, b.getPhotographerId());
        st.setLong(3, b.getHallId());
        st.setString(4, b.getSessionType().name());
        st.setString(5, b.getStatus().name());
        st.setTimestamp(6, Timestamp.valueOf(b.getStartTime()));
        st.setTimestamp(7, Timestamp.valueOf(b.getEndTime()));
        st.setInt(8, b.getParticipants());
        st.setBigDecimal(9, b.getTotalPrice());
        st.setString(10, b.getComment());
        st.setLong(11, b.getId());
    }

    @Override
    protected Booking map(ResultSet rs) throws SQLException {
        return new Booking(rs.getLong("id"), rs.getLong("client_id"), rs.getLong("photographer_id"),
                rs.getLong("hall_id"), SessionType.valueOf(rs.getString("session_type")),
                BookingStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("start_time").toLocalDateTime(), rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getInt("participants"), rs.getBigDecimal("total_price"), rs.getString("comment"),
                rs.getTimestamp("created_at").toLocalDateTime());
    }

    @Override
    public List<Booking> findByDate(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return query("SELECT * FROM bookings WHERE start_time >= ? AND start_time < ? ORDER BY start_time", st -> {
            st.setTimestamp(1, Timestamp.valueOf(from));
            st.setTimestamp(2, Timestamp.valueOf(to));
        });
    }

    @Override
    public List<Booking> findActiveOverlappingForPhotographer(long photographerId, LocalDateTime start,
                                                              LocalDateTime end, long excludeId) {
        return query("SELECT * FROM bookings WHERE photographer_id = ?" + OVERLAP_CONDITION, st -> {
            st.setLong(1, photographerId);
            st.setTimestamp(2, Timestamp.valueOf(end));
            st.setTimestamp(3, Timestamp.valueOf(start));
            st.setLong(4, excludeId);
        });
    }

    @Override
    public List<Booking> findActiveOverlappingForHall(long hallId, LocalDateTime start,
                                                      LocalDateTime end, long excludeId) {
        return query("SELECT * FROM bookings WHERE hall_id = ?" + OVERLAP_CONDITION, st -> {
            st.setLong(1, hallId);
            st.setTimestamp(2, Timestamp.valueOf(end));
            st.setTimestamp(3, Timestamp.valueOf(start));
            st.setLong(4, excludeId);
        });
    }

    @Override
    public boolean existsByClientId(long clientId) {
        return exists("SELECT 1 FROM bookings WHERE client_id = ? LIMIT 1", st -> st.setLong(1, clientId));
    }

    @Override
    public boolean existsByPhotographerId(long photographerId) {
        return exists("SELECT 1 FROM bookings WHERE photographer_id = ? LIMIT 1", st -> st.setLong(1, photographerId));
    }

    @Override
    public boolean existsByHallId(long hallId) {
        return exists("SELECT 1 FROM bookings WHERE hall_id = ? LIMIT 1", st -> st.setLong(1, hallId));
    }
}
