package ru.mirea.photostudio.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingStatusTest {

    @Test
    void createdCanBeConfirmedOrCancelledOnly() {
        assertEquals(List.of(BookingStatus.CONFIRMED, BookingStatus.CANCELLED), BookingStatus.CREATED.nextStatuses());
        assertFalse(BookingStatus.CREATED.canTransitionTo(BookingStatus.COMPLETED));
    }

    @Test
    void confirmedCanBeCompletedOrCancelled() {
        assertTrue(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.COMPLETED));
        assertTrue(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CANCELLED));
        assertFalse(BookingStatus.CONFIRMED.canTransitionTo(BookingStatus.CREATED));
    }

    @Test
    void finalStatusesHaveNoTransitions() {
        assertTrue(BookingStatus.COMPLETED.nextStatuses().isEmpty());
        assertTrue(BookingStatus.CANCELLED.nextStatuses().isEmpty());
        assertTrue(BookingStatus.COMPLETED.isFinal());
    }
}
