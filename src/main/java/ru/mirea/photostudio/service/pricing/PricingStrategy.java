package ru.mirea.photostudio.service.pricing;

import ru.mirea.photostudio.model.Hall;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Интерфейс стратегии расчёта стоимости фотосессии (паттерн «Стратегия»).
 * Разные реализации по-разному считают цену, а BookingService работает с ними одинаково — это полиморфизм.
 */
public interface PricingStrategy {

    /** Подходит ли эта стратегия для данной брони. */
    boolean isApplicable(LocalDateTime start, int hours);

    BigDecimal calculate(Hall hall, int hours);

    /** Название тарифа (для информации). */
    String getName();
}
