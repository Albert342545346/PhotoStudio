package ru.mirea.photostudio.model;

import ru.mirea.photostudio.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Бронирование фотосессии — основная сущность варианта «Фотостудия».
 * Связана с Client (client_id) и Hall (hall_id).
 * Поля clientName и hallName заполняются репозиторием через JOIN и нужны только для красивого вывода.
 */
public class Booking extends BaseEntity {

    private int clientId;
    private int hallId;
    private String clientName;
    private String hallName;
    private String title;
    private String description;
    private SessionType sessionType;
    private LocalDateTime startTime;
    private int durationHours;
    private BigDecimal price;
    private BookingStatus status;
    private LocalDateTime createdAt;

    /** Конструктор для новой брони: статус всегда CREATED, ID и дату создания назначит БД. */
    public Booking(int clientId, int hallId, String title, String description,
                   SessionType sessionType, LocalDateTime startTime, int durationHours, BigDecimal price) {
        this(0, clientId, hallId, title, description, sessionType, startTime,
                durationHours, price, BookingStatus.CREATED, null);
    }

    /** Конструктор для брони, прочитанной из базы данных. */
    public Booking(int id, int clientId, int hallId, String title, String description,
                   SessionType sessionType, LocalDateTime startTime, int durationHours,
                   BigDecimal price, BookingStatus status, LocalDateTime createdAt) {
        super(id);
        this.clientId = clientId;
        this.hallId = hallId;
        this.title = title;
        this.description = description;
        this.sessionType = sessionType;
        this.startTime = startTime;
        this.durationHours = durationHours;
        this.price = price;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getClientId() {
        return clientId;
    }

    public int getHallId() {
        return hallId;
    }

    public void setHallId(int hallId) {
        this.hallId = hallId;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getHallName() {
        return hallName;
    }

    public void setHallName(String hallName) {
        this.hallName = hallName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public int getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(int durationHours) {
        this.durationHours = durationHours;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /** Время окончания вычисляется, а не хранится в БД. */
    public LocalDateTime getEndTime() {
        return startTime.plusHours(durationHours);
    }

    @Override
    public String describe() {
        String desc = (description == null || description.isBlank()) ? "—" : description;
        return String.format("#%d  %s  [%s]%n"
                        + "    Клиент: %s | Зал: %s | Тип: %s%n"
                        + "    Время: %s–%s (%d ч) | Стоимость: %s%n"
                        + "    Описание: %s",
                getId(), title, status.getTitle(),
                clientName, hallName, sessionType.getTitle(),
                startTime.format(Formats.DATE_TIME), getEndTime().format(Formats.TIME), durationHours,
                Formats.money(price), desc);
    }
}
