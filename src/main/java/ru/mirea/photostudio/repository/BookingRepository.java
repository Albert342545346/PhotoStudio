package ru.mirea.photostudio.repository;

import ru.mirea.photostudio.exception.DatabaseException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Работа с таблицей bookings через JDBC.
 * Все запросы параметризованные (PreparedStatement с «?»), значения никогда не подставляются в SQL строкой.
 */
public class BookingRepository implements CrudRepository<Booking> {

    /** Базовый SELECT с JOIN: сразу получаем имя клиента и название зала. */
    private static final String SELECT_JOINED = """
            SELECT b.id, b.client_id, c.full_name AS client_name, b.hall_id, h.name AS hall_name,
                   b.title, b.description, b.session_type, b.start_time, b.duration_hours,
                   b.price, b.status, b.created_at
            FROM bookings b
            JOIN clients c ON c.id = b.client_id
            JOIN halls h ON h.id = b.hall_id
            """;

    /** Функциональный интерфейс: «как подставить параметры в PreparedStatement». Позволяет писать запросы лямбдами. */
    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    // ------------------------------------------------------------------ CRUD

    @Override
    public Booking save(Booking booking) {
        String sql = """
                INSERT INTO bookings (client_id, hall_id, title, description, session_type,
                                      start_time, duration_hours, price, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"})) {
            ps.setInt(1, booking.getClientId());
            ps.setInt(2, booking.getHallId());
            ps.setString(3, booking.getTitle());
            ps.setString(4, booking.getDescription());
            ps.setString(5, booking.getSessionType().name());
            ps.setTimestamp(6, Timestamp.valueOf(booking.getStartTime()));
            ps.setInt(7, booking.getDurationHours());
            ps.setBigDecimal(8, booking.getPrice());
            ps.setString(9, booking.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    booking.setId(keys.getInt(1));
                }
            }
            return booking;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при добавлении бронирования", e);
        }
    }

    @Override
    public Optional<Booking> findById(int id) {
        List<Booking> found = query(SELECT_JOINED + " WHERE b.id = ?", ps -> ps.setInt(1, id),
                "Ошибка выполнения SQL при поиске бронирования");
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    @Override
    public List<Booking> findAll() {
        return query(SELECT_JOINED + " ORDER BY b.id", ps -> { }, "Ошибка выполнения SQL при чтении бронирований");
    }

    @Override
    public void update(Booking booking) {
        String sql = """
                UPDATE bookings
                SET hall_id = ?, title = ?, description = ?, session_type = ?,
                    start_time = ?, duration_hours = ?, price = ?, status = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, booking.getHallId());
            ps.setString(2, booking.getTitle());
            ps.setString(3, booking.getDescription());
            ps.setString(4, booking.getSessionType().name());
            ps.setTimestamp(5, Timestamp.valueOf(booking.getStartTime()));
            ps.setInt(6, booking.getDurationHours());
            ps.setBigDecimal(7, booking.getPrice());
            ps.setString(8, booking.getStatus().name());
            ps.setInt(9, booking.getId());
            if (ps.executeUpdate() == 0) {
                throw new EntityNotFoundException("Бронирование", booking.getId());
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при изменении бронирования", e);
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM bookings WHERE id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при удалении бронирования", e);
        }
    }

    // ---------------------------------------------------------------- поиск

    public List<Booking> searchByTitle(String text) {
        return query(SELECT_JOINED + " WHERE LOWER(b.title) LIKE LOWER(?) ORDER BY b.start_time",
                ps -> ps.setString(1, "%" + text + "%"), "Ошибка выполнения SQL при поиске по названию");
    }

    public List<Booking> searchByDescription(String text) {
        return query(SELECT_JOINED + " WHERE LOWER(b.description) LIKE LOWER(?) ORDER BY b.start_time",
                ps -> ps.setString(1, "%" + text + "%"), "Ошибка выполнения SQL при поиске по описанию");
    }

    public List<Booking> searchByClientName(String text) {
        return query(SELECT_JOINED + " WHERE LOWER(c.full_name) LIKE LOWER(?) ORDER BY b.start_time",
                ps -> ps.setString(1, "%" + text + "%"), "Ошибка выполнения SQL при поиске по клиенту");
    }

    /** Все съёмки, начинающиеся в указанный день. */
    public List<Booking> searchByDate(LocalDate date) {
        return query(SELECT_JOINED + " WHERE b.start_time >= ? AND b.start_time < ? ORDER BY b.start_time",
                ps -> {
                    ps.setTimestamp(1, Timestamp.valueOf(date.atStartOfDay()));
                    ps.setTimestamp(2, Timestamp.valueOf(date.plusDays(1).atStartOfDay()));
                }, "Ошибка выполнения SQL при поиске по дате");
    }

    // ---------------------------------------------------- вспомогательные

    /** Все брони конкретного зала (для проверки пересечения по времени). */
    public List<Booking> findByHallId(int hallId) {
        return query(SELECT_JOINED + " WHERE b.hall_id = ? ORDER BY b.start_time",
                ps -> ps.setInt(1, hallId), "Ошибка выполнения SQL при чтении броней зала");
    }

    public boolean existsByClientId(int clientId) {
        String sql = "SELECT COUNT(*) FROM bookings WHERE client_id = ?";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при проверке броней клиента", e);
        }
    }

    /** Количество броней по статусам (SQL-агрегация GROUP BY). */
    public Map<BookingStatus, Integer> countByStatus() {
        Map<BookingStatus, Integer> result = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            result.put(status, 0);
        }
        String sql = "SELECT status, COUNT(*) AS cnt FROM bookings GROUP BY status";
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.put(BookingStatus.valueOf(rs.getString("status")), rs.getInt("cnt"));
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения SQL при подсчёте броней по статусам", e);
        }
        return result;
    }

    // ------------------------------------------------------------ внутреннее

    /** Универсальное выполнение SELECT: открывает соединение, подставляет параметры, собирает список. */
    private List<Booking> query(String sql, ParamSetter setter, String errorMessage) {
        List<Booking> result = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException(errorMessage, e);
        }
        return result;
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        Booking booking = new Booking(
                rs.getInt("id"),
                rs.getInt("client_id"),
                rs.getInt("hall_id"),
                rs.getString("title"),
                rs.getString("description"),
                SessionType.valueOf(rs.getString("session_type")),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getInt("duration_hours"),
                rs.getBigDecimal("price"),
                BookingStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toLocalDateTime());
        booking.setClientName(rs.getString("client_name"));
        booking.setHallName(rs.getString("hall_name"));
        return booking;
    }
}
