package ru.mirea.photostudio.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

/** Основная сущность: бронирование фотосессии. Связывает клиента, фотографа и зал. */
public class Booking implements Identifiable {
    private long id;
    private long clientId;
    private long photographerId;
    private long hallId;
    private SessionType sessionType;
    private BookingStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private int participants;
    private BigDecimal totalPrice;
    private String comment;
    private LocalDateTime createdAt;

    public Booking(long id, long clientId, long photographerId, long hallId, SessionType sessionType,
                   BookingStatus status, LocalDateTime startTime, LocalDateTime endTime, int participants,
                   BigDecimal totalPrice, String comment, LocalDateTime createdAt) {
        this.id = id;
        this.clientId = clientId;
        this.photographerId = photographerId;
        this.hallId = hallId;
        this.sessionType = sessionType;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participants = participants;
        this.totalPrice = totalPrice;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    /** Конструктор для нового бронирования: статус и цену выставляет сервис. */
    public Booking(long clientId, long photographerId, long hallId, SessionType sessionType,
                   LocalDateTime startTime, LocalDateTime endTime, int participants, String comment) {
        this(0, clientId, photographerId, hallId, sessionType, BookingStatus.CREATED, startTime, endTime,
                participants, BigDecimal.ZERO, comment, LocalDateTime.now());
    }

    /** Длительность в минутах (0, если время не задано). */
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return Duration.between(startTime, endTime).toMinutes();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public long getPhotographerId() {
        return photographerId;
    }

    public void setPhotographerId(long photographerId) {
        this.photographerId = photographerId;
    }

    public long getHallId() {
        return hallId;
    }

    public void setHallId(long hallId) {
        this.hallId = hallId;
    }

    public SessionType getSessionType() {
        return sessionType;
    }

    public void setSessionType(SessionType sessionType) {
        this.sessionType = sessionType;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public int getParticipants() {
        return participants;
    }

    public void setParticipants(int participants) {
        this.participants = participants;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Бронирование #" + id + " (" + status + ", " + startTime + ")";
    }
}
