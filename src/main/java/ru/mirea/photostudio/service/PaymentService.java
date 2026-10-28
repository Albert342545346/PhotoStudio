package ru.mirea.photostudio.service;

import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Бизнес-правила платежей: бронирование существует и не отменено, сумма положительная,
 * сумма не превышает остаток к оплате, платёж по завершённому бронированию удалять нельзя.
 */
public class PaymentService {
    private final PaymentRepository payments;
    private final BookingRepository bookings;

    public PaymentService(PaymentRepository payments, BookingRepository bookings) {
        this.payments = payments;
        this.bookings = bookings;
    }

    public Payment create(Payment payment) {
        Booking booking = bookings.findById(payment.getBookingId()).orElseThrow(() ->
                new BusinessException("Нельзя принять платёж по несуществующему бронированию (ID "
                        + payment.getBookingId() + ")"));
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BusinessException("Нельзя принять платёж по отменённому бронированию");
        }
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0) {
            throw new BusinessException("Сумма платежа должна быть больше нуля");
        }
        if (payment.getMethod() == null) {
            throw new BusinessException("Не указан способ оплаты");
        }
        BigDecimal remaining = remainingToPay(booking);
        if (payment.getAmount().compareTo(remaining) > 0) {
            throw new BusinessException("Сумма платежа превышает остаток к оплате (" + remaining + ")");
        }
        payment.setPaidAt(LocalDateTime.now());
        return payments.create(payment);
    }

    public List<Payment> getAll() {
        return payments.findAll();
    }

    public Payment getById(long id) {
        return payments.findById(id).orElseThrow(() -> new EntityNotFoundException("Платёж", id));
    }

    public List<Payment> getByBooking(long bookingId) {
        bookings.findById(bookingId).orElseThrow(() -> new EntityNotFoundException("Бронирование", bookingId));
        return payments.findByBookingId(bookingId);
    }

    public BigDecimal remainingToPay(Booking booking) {
        return booking.getTotalPrice().subtract(payments.sumByBookingId(booking.getId()));
    }

    public void delete(long id) {
        Payment payment = getById(id);
        Booking booking = bookings.findById(payment.getBookingId()).orElseThrow(() ->
                new EntityNotFoundException("Бронирование", payment.getBookingId()));
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BusinessException("Нельзя удалить платёж по завершённому бронированию");
        }
        payments.deleteById(id);
    }
}
