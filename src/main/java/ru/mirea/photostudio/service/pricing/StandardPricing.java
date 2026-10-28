package ru.mirea.photostudio.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Базовая цена: часы × (ставка фотографа + цена зала) × коэффициент типа съёмки. */
public class StandardPricing implements PricingStrategy {
    @Override
    public BigDecimal calculate(PricingContext ctx) {
        return ctx.hours()
                .multiply(ctx.photographerRate().add(ctx.hallPrice()))
                .multiply(ctx.sessionType().getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
