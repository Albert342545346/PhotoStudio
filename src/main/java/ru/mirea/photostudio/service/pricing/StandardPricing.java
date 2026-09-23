package ru.mirea.photostudio.service.pricing;

import ru.mirea.photostudio.model.Hall;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Обычный тариф: ставка зала x количество часов. Подходит всегда (запасной вариант). */
public class StandardPricing implements PricingStrategy {

    @Override
    public boolean isApplicable(LocalDateTime start, int hours) {
        return true;
    }

    @Override
    public BigDecimal calculate(Hall hall, int hours) {
        return hall.getPricePerHour().multiply(BigDecimal.valueOf(hours));
    }

    @Override
    public String getName() {
        return "Стандартный тариф";
    }
}
