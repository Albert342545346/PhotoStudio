package ru.mirea.photostudio.service;

import org.junit.jupiter.api.Test;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.pricing.PricingContext;
import ru.mirea.photostudio.service.pricing.PricingStrategy;
import ru.mirea.photostudio.service.pricing.StandardPricing;
import ru.mirea.photostudio.service.pricing.VipDiscountPricing;
import ru.mirea.photostudio.service.pricing.WeekendPricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingTest {
    private final PricingStrategy pricing = new VipDiscountPricing(new WeekendPricing(new StandardPricing()));

    private PricingContext ctx(SessionType type, String hours, LocalDateTime start, boolean vip) {
        return new PricingContext(type, new BigDecimal(hours), new BigDecimal("2000"), new BigDecimal("1500"), start, vip);
    }

    @Test
    void weekdayRegularClientPaysBasePrice() {
        // 1.5 ч × (2000 + 1500) × 1.00
        assertEquals(new BigDecimal("5250.00"),
                pricing.calculate(ctx(SessionType.PORTRAIT, "1.50", LocalDateTime.of(2026, 10, 8, 15, 0), false)));
    }

    @Test
    void weekendAddsTwentyPercent() {
        // суббота: 2 ч × 3500 × 1.00 × 1.20
        assertEquals(new BigDecimal("8400.00"),
                pricing.calculate(ctx(SessionType.PORTRAIT, "2.00", LocalDateTime.of(2026, 9, 5, 11, 0), false)));
    }

    @Test
    void vipGetsTenPercentDiscountOnTopOfWeekend() {
        assertEquals(new BigDecimal("7560.00"),
                pricing.calculate(ctx(SessionType.PORTRAIT, "2.00", LocalDateTime.of(2026, 9, 5, 11, 0), true)));
    }

    @Test
    void sessionTypeCoefficientIsApplied() {
        // вторник: 1 ч × 3500 × 1.50
        assertEquals(new BigDecimal("5250.00"),
                pricing.calculate(ctx(SessionType.WEDDING, "1.00", LocalDateTime.of(2026, 11, 3, 12, 0), false)));
    }
}
