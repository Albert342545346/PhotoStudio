package ru.mirea.photostudio.repository;

import ru.mirea.photostudio.model.Payment;

import java.math.BigDecimal;
import java.util.List;

public interface PaymentRepository extends CrudRepository<Payment> {
    List<Payment> findByBookingId(long bookingId);

    boolean existsByBookingId(long bookingId);

    /** Сумма платежей по бронированию (0, если платежей нет). */
    BigDecimal sumByBookingId(long bookingId);

    /** Сумма всех платежей. */
    BigDecimal sumAll();
}
