package ru.mirea.photostudio.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.PaymentMethod;
import ru.mirea.photostudio.service.BookingView;

import java.math.BigDecimal;
import java.util.List;

/** Форма приёма платежа. В списке только неотменённые бронирования с остатком к оплате. */
public class PaymentDialogController extends DialogController<Payment> {
    private final AppContext ctx = AppContext.get();

    @FXML
    private ComboBox<BookingView> bookingCombo;
    @FXML
    private Label remainingLabel;
    @FXML
    private TextField amountField;
    @FXML
    private ComboBox<PaymentMethod> methodCombo;
    @FXML
    private Label errorLabel;

    @FXML
    private void initialize() {
        List<BookingView> payable = ctx.bookingService.toViews(ctx.bookingService.getAll()).stream()
                .filter(v -> v.booking().getStatus() != BookingStatus.CANCELLED)
                .filter(v -> v.remaining().signum() > 0)
                .toList();
        FxUtil.configureCombo(bookingCombo, v -> v.label() + " · остаток " + FxUtil.money(v.remaining()),
                "Выберите бронирование");
        bookingCombo.setItems(FXCollections.observableArrayList(payable));
        FxUtil.configureCombo(methodCombo, PaymentMethod::getTitle, "Выберите способ оплаты");
        methodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        methodCombo.setValue(PaymentMethod.CARD);
        remainingLabel.setText("—");
        bookingCombo.valueProperty().addListener((o, a, view) ->
                remainingLabel.setText(view == null ? "—" : FxUtil.money(view.remaining())));
        FxUtil.clearFormError(errorLabel);
    }

    public void setPreselectedBookingId(long bookingId) {
        bookingCombo.getItems().stream().filter(v -> v.booking().getId() == bookingId).findFirst()
                .ifPresent(bookingCombo::setValue);
    }

    @FXML
    private void onPayFull() {
        BookingView view = bookingCombo.getValue();
        if (view != null) {
            amountField.setText(view.remaining().toPlainString());
        }
    }

    @FXML
    private void onSave() {
        try {
            FxUtil.clearFormError(errorLabel);
            BookingView view = FxUtil.requireSelected(bookingCombo.getValue(), "Бронирование");
            BigDecimal amount = FxUtil.parseDecimal(amountField.getText(), "Сумма");
            PaymentMethod method = FxUtil.requireSelected(methodCombo.getValue(), "Способ оплаты");
            finish(ctx.paymentService.create(new Payment(view.booking().getId(), amount, method)));
        } catch (RuntimeException e) {
            FxUtil.showFormError(errorLabel, e);
        }
    }
}
