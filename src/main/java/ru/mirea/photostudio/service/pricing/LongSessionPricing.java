package ru.mirea.photostudio.service.pricing;

import ru.mirea.photostudio.model.Hall;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/** Скидка 10% на длительные съёмки (4 часа и более) в будние дни. */
public class LongSessionPricing implements PricingStrategy {

    private static final int MIN_HOURS = 4;
    private static final BigDecimal COEFFICIENT = new BigDecimal("0.90");

    @Override
    public boolean isApplicable(LocalDateTime start, int hours) {
        return hours >= MIN_HOURS;
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
        return "Скидка за длительную съёмку (-10%)";
    }
}
