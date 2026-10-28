package ru.mirea.photostudio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Платёж по бронированию. */
public class Payment implements Identifiable {
    private long id;
    private long bookingId;
    private BigDecimal amount;
    private PaymentMethod method;
    private LocalDateTime paidAt;

    public Payment(long id, long bookingId, BigDecimal amount, PaymentMethod method, LocalDateTime paidAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.amount = amount;
        this.method = method;
        this.paidAt = paidAt;
    }

    public Payment(long bookingId, BigDecimal amount, PaymentMethod method) {
        this(0, bookingId, amount, method, LocalDateTime.now());
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    public long getBookingId() {
        return bookingId;
    }

    public void setBookingId(long bookingId) {
        this.bookingId = bookingId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    @Override
    public String toString() {
        return "Платёж #" + id + " по бронированию #" + bookingId + " на " + amount;
    }
}
