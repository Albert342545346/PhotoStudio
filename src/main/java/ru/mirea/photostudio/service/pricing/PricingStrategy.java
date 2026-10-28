package ru.mirea.photostudio.service.pricing;

import java.math.BigDecimal;

/** Стратегия расчёта стоимости. Реализации комбинируются (паттерн «Декоратор») — полиморфизм. */
public interface PricingStrategy {
    BigDecimal calculate(PricingContext context);
}
