package ru.mirea.photostudio.service.pricing;

import ru.mirea.photostudio.model.Hall;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDateTime;

/** Выходной тариф: суббота и воскресенье дороже на 20%. */
public class WeekendPricing implements PricingStrategy {

    private static final BigDecimal COEFFICIENT = new BigDecimal("1.20");

    @Override
    public boolean isApplicable(LocalDateTime start, int hours) {
        DayOfWeek day = start.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    @Override
    public BigDecimal calculate(Hall hall, int hours) {
        return hall.getPricePerHour()
                .multiply(BigDecimal.valueOf(hours))
                .multiply(COEFFICIENT)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getName() {
        return "Выходной тариф (+20%)";
    }
}
