package ru.mirea.photostudio.service.pricing;

import ru.mirea.photostudio.model.SessionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Данные, необходимые для расчёта стоимости бронирования. */
public record PricingContext(SessionType sessionType, BigDecimal hours, BigDecimal photographerRate,
                             BigDecimal hallPrice, LocalDateTime start, boolean vipClient) {
}
