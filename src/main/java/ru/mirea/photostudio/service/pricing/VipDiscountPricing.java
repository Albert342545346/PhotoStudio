package ru.mirea.photostudio.service.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Скидка 10% для VIP-клиентов. */
public class VipDiscountPricing implements PricingStrategy {
    private static final BigDecimal DISCOUNT_FACTOR = new BigDecimal("0.90");
    private final PricingStrategy delegate;

    public VipDiscountPricing(PricingStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public BigDecimal calculate(PricingContext ctx) {
        BigDecimal price = delegate.calculate(ctx);
        if (ctx.vipClient()) {
            return price.multiply(DISCOUNT_FACTOR).setScale(2, RoundingMode.HALF_UP);
        }
        return price;
    }
}
