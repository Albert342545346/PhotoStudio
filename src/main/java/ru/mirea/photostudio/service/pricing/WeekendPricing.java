package ru.mirea.photostudio.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;

/** Наценка 20% за съёмку в субботу и воскресенье. */
public class WeekendPricing implements PricingStrategy {
    private static final BigDecimal MARKUP = new BigDecimal("1.20");
    private final PricingStrategy delegate;

    public WeekendPricing(PricingStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public BigDecimal calculate(PricingContext ctx) {
        BigDecimal price = delegate.calculate(ctx);
        DayOfWeek day = ctx.start().getDayOfWeek();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
            return price.multiply(MARKUP).setScale(2, RoundingMode.HALF_UP);
        }
        return price;
    }
}
