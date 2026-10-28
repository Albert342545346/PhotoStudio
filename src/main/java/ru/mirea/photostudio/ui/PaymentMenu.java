package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.PaymentMethod;
import ru.mirea.photostudio.service.PaymentService;

import java.math.BigDecimal;
import java.util.List;

public class PaymentMenu extends AbstractMenu {
    private final PaymentService service;

    public PaymentMenu(ConsoleInput in, ConsolePrinter out, PaymentService service) {
        super(in, out);
        this.service = service;
    }

    @Override
    protected String title() {
        return "ПЛАТЕЖИ";
    }

    @Override
    protected List<String> items() {
        return List.of("1. Принять платёж", "2. Показать все платежи", "3. Найти платёж по ID",
                "4. Платежи по бронированию", "5. Удалить платёж");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> {
                long bookingId = in.id("ID бронирования: ");
                BigDecimal amount = in.decimal("Сумма, руб.: ", "Сумма");
                PaymentMethod method = in.choose("Способ оплаты:", PaymentMethod.values());
                Payment created = service.create(new Payment(bookingId, amount, method));
                System.out.println("Платёж принят, ID = " + created.getId());
            }
            case "2" -> out.payments(service.getAll());
            case "3" -> out.payments(List.of(service.getById(in.id("Введите ID: "))));
            case "4" -> out.payments(service.getByBooking(in.id("ID бронирования: ")));
            case "5" -> {
                service.delete(in.id("Введите ID платежа: "));
                System.out.println("Платёж удалён.");
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
