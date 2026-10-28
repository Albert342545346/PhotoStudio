package ru.mirea.photostudio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mirea.photostudio.exception.BusinessException;
import ru.mirea.photostudio.exception.EntityNotFoundException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingSortField;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.PaymentMethod;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.pricing.StandardPricing;
import ru.mirea.photostudio.service.pricing.VipDiscountPricing;
import ru.mirea.photostudio.service.pricing.WeekendPricing;
import ru.mirea.photostudio.support.InMemoryRepositories.Bookings;
import ru.mirea.photostudio.support.InMemoryRepositories.Clients;
import ru.mirea.photostudio.support.InMemoryRepositories.Halls;
import ru.mirea.photostudio.support.InMemoryRepositories.Payments;
import ru.mirea.photostudio.support.InMemoryRepositories.Photographers;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingServiceTest {
    private Bookings bookings;
    private Payments payments;
    private BookingService service;
    private PaymentService paymentService;
    private LocalDateTime noon;

    @BeforeEach
    void setUp() {
        Clients clients = new Clients();
        Photographers photographers = new Photographers();
        Halls halls = new Halls();
        bookings = new Bookings();
        payments = new Payments();
        clients.create(new Client("Иванов Иван", "+79160000001", "ivan@mail.ru", false));
        clients.create(new Client("Петрова Анна", "+79160000002", "anna@mail.ru", true));
        photographers.create(new Photographer("Орлов Сергей", "+79030000001", "orlov@ps.ru",
                SessionType.PORTRAIT, new BigDecimal("2000"), true));
        photographers.create(new Photographer("Белов Кирилл", "+79030000002", "belov@ps.ru",
                SessionType.PORTRAIT, new BigDecimal("1900"), false));
        halls.create(new Hall("Белый зал", 5, new BigDecimal("1000"), ""));
        service = new BookingService(bookings, clients, photographers, halls, payments,
                new VipDiscountPricing(new WeekendPricing(new StandardPricing())));
        paymentService = new PaymentService(payments, bookings);
        // будний день через 10+ дней, 12:00
        LocalDate day = LocalDate.now().plusDays(10);
        while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.plusDays(1);
        }
        noon = day.atTime(12, 0);
    }

    private Booking draft(long clientId, long photographerId, LocalDateTime start, int hours, int people) {
        return new Booking(clientId, photographerId, 1, SessionType.PORTRAIT, start, start.plusHours(hours), people, "");
    }

    @Test
    void createsBookingWithCalculatedPriceAndCreatedStatus() {
        Booking created = service.create(draft(1, 1, noon, 2, 3));
        assertEquals(BookingStatus.CREATED, created.getStatus());
        assertEquals(new BigDecimal("6000.00"), created.getTotalPrice()); // 2 ч × (2000 + 1000)
    }

    @Test
    void rejectsUnknownClient() {
        assertThrows(BusinessException.class, () -> service.create(draft(99, 1, noon, 2, 1)));
    }

    @Test
    void rejectsInactivePhotographer() {
        assertThrows(BusinessException.class, () -> service.create(draft(1, 2, noon, 2, 1)));
    }

    @Test
    void rejectsPastStartTime() {
        assertThrows(BusinessException.class,
                () -> service.create(draft(1, 1, LocalDate.now().minusDays(1).atTime(12, 0), 2, 1)));
    }

    @Test
    void rejectsTimeOutsideWorkingHours() {
        assertThrows(BusinessException.class,
                () -> service.create(draft(1, 1, noon.withHour(7), 2, 1)));
        assertThrows(BusinessException.class,
                () -> service.create(draft(1, 1, noon.withHour(20), 2, 1)));
    }

    @Test
    void rejectsWrongDuration() {
        assertThrows(BusinessException.class,
                () -> service.create(new Booking(1, 1, 1, SessionType.PORTRAIT, noon, noon.plusMinutes(30), 1, "")));
        assertThrows(BusinessException.class,
                () -> service.create(draft(1, 1, noon.withHour(9), 9, 1)));
    }

    @Test
    void rejectsMoreParticipantsThanHallCapacity() {
        assertThrows(BusinessException.class, () -> service.create(draft(1, 1, noon, 2, 6)));
    }

    @Test
    void rejectsOverlappingBookingForSamePhotographer() {
        service.create(draft(1, 1, noon, 2, 1));
        assertThrows(BusinessException.class, () -> service.create(draft(2, 1, noon.plusHours(1), 2, 1)));
    }

    @Test
    void allowsBackToBackBookings() {
        service.create(draft(1, 1, noon, 2, 1));
        service.create(draft(2, 1, noon.plusHours(2), 2, 1));
        assertEquals(2, service.getAll().size());
    }

    @Test
    void statusTransitionsAreValidated() {
        Booking b = service.create(draft(1, 1, noon, 2, 1));
        assertThrows(BusinessException.class, () -> service.changeStatus(b.getId(), BookingStatus.COMPLETED));
        service.changeStatus(b.getId(), BookingStatus.CONFIRMED);
        // съёмка в будущем — завершить нельзя
        assertThrows(BusinessException.class, () -> service.changeStatus(b.getId(), BookingStatus.COMPLETED));
        service.changeStatus(b.getId(), BookingStatus.CANCELLED);
        assertThrows(BusinessException.class, () -> service.update(service.getById(b.getId())));
    }

    @Test
    void cannotDeleteConfirmedBookingOrBookingWithPayments() {
        Booking confirmed = service.create(draft(1, 1, noon, 2, 1));
        service.changeStatus(confirmed.getId(), BookingStatus.CONFIRMED);
        assertThrows(BusinessException.class, () -> service.delete(confirmed.getId()));

        Booking withPayment = service.create(draft(2, 1, noon.plusHours(3), 1, 1));
        paymentService.create(new Payment(withPayment.getId(), new BigDecimal("100"), PaymentMethod.CASH));
        assertThrows(BusinessException.class, () -> service.delete(withPayment.getId()));
    }

    @Test
    void deletesFreshBooking() {
        Booking b = service.create(draft(1, 1, noon, 2, 1));
        service.delete(b.getId());
        assertThrows(EntityNotFoundException.class, () -> service.getById(b.getId()));
    }

    @Test
    void paymentCannotExceedRemainingAmount() {
        Booking b = service.create(draft(1, 1, noon, 2, 1)); // 6000.00
        paymentService.create(new Payment(b.getId(), new BigDecimal("4000"), PaymentMethod.CARD));
        assertThrows(BusinessException.class,
                () -> paymentService.create(new Payment(b.getId(), new BigDecimal("2500"), PaymentMethod.CARD)));
        paymentService.create(new Payment(b.getId(), new BigDecimal("2000"), PaymentMethod.CARD));
        assertEquals(0, service.getView(b.getId()).remaining().signum());
    }

    @Test
    void queryFiltersSearchesAndSorts() {
        Booking first = service.create(draft(1, 1, noon, 2, 1));
        Booking second = service.create(draft(2, 1, noon.plusDays(1).withHour(10), 1, 1));
        service.changeStatus(second.getId(), BookingStatus.CONFIRMED);

        assertEquals(2, service.query(BookingQuery.all()).size());

        List<BookingView> confirmed = service.query(new BookingQuery(null, BookingStatus.CONFIRMED, null, null, null,
                BookingSortField.START_TIME, true));
        assertEquals(1, confirmed.size());
        assertEquals(second.getId(), confirmed.get(0).booking().getId());

        List<BookingView> byName = service.query(new BookingQuery("петров", null, null, null, null,
                BookingSortField.START_TIME, true));
        assertEquals(1, byName.size());

        List<BookingView> byPriceDesc = service.query(new BookingQuery(null, null, null, null, null,
                BookingSortField.PRICE, false));
        assertEquals(first.getId(), byPriceDesc.get(0).booking().getId()); // 6000 > 3000 × 0.9

        assertThrows(BusinessException.class, () -> service.query(new BookingQuery(null, null, null,
                LocalDate.now().plusDays(5), LocalDate.now(), BookingSortField.START_TIME, true)));
    }

    @Test
    void estimatePriceMatchesCreatedPrice() {
        Booking draft = draft(2, 1, noon, 2, 1);
        BigDecimal estimate = service.estimatePrice(draft);
        assertEquals(estimate, service.create(draft).getTotalPrice());
        assertTrue(estimate.signum() > 0);
    }
}
