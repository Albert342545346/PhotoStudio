package ru.mirea.photostudio.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.Dialogs;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.PaymentMethod;
import ru.mirea.photostudio.service.BookingView;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Вкладка «Платежи»: таблица с названием бронирования, фильтр по способу оплаты, приём и удаление платежа. */
public class PaymentsController extends TabController {
    private final AppContext ctx = AppContext.get();

    /** Строка таблицы: платёж и подпись связанного бронирования. */
    private record PaymentRow(Payment payment, String bookingLabel) {
    }

    @FXML
    private ComboBox<PaymentMethod> methodCombo;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<PaymentRow> table;
    @FXML
    private TableColumn<PaymentRow, String> idColumn;
    @FXML
    private TableColumn<PaymentRow, String> bookingColumn;
    @FXML
    private TableColumn<PaymentRow, String> amountColumn;
    @FXML
    private TableColumn<PaymentRow, String> methodColumn;
    @FXML
    private TableColumn<PaymentRow, String> dateColumn;
    @FXML
    private Label countLabel;

    @Override
    protected Node anchor() {
        return table;
    }

    @FXML
    private void initialize() {
        FxUtil.configureCombo(methodCombo, PaymentMethod::getTitle, "Любой способ оплаты");
        methodCombo.setItems(FXCollections.observableArrayList(PaymentMethod.values()));
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().payment().getId())));
        bookingColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().bookingLabel()));
        amountColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.money(c.getValue().payment().getAmount())));
        methodColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().payment().getMethod().getTitle()));
        dateColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.dateTime(c.getValue().payment().getPaidAt())));
        table.setPlaceholder(new Label("Платежей нет"));
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        methodCombo.valueProperty().addListener((obs, a, b) -> refresh());
        refresh();
    }

    @Override
    public void refresh() {
        try {
            Map<Long, String> labels = ctx.bookingService.toViews(ctx.bookingService.getAll()).stream()
                    .collect(Collectors.toMap(v -> v.booking().getId(), BookingView::label));
            PaymentMethod method = methodCombo.getValue();
            List<PaymentRow> rows = ctx.paymentService.getAll().stream()
                    .filter(p -> method == null || p.getMethod() == method)
                    .map(p -> new PaymentRow(p, labels.getOrDefault(p.getBookingId(), "№" + p.getBookingId())))
                    .toList();
            table.setItems(FXCollections.observableArrayList(rows));
            BigDecimal total = rows.stream().map(r -> r.payment().getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
            countLabel.setText("Платежей: " + rows.size() + " · на сумму " + FxUtil.money(total));
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onAdd() {
        try {
            PaymentDialogController dialog = Dialogs.openModal(owner(), "/fxml/payment-dialog.fxml", "Приём платежа",
                    (PaymentDialogController d) -> { });
            if (dialog.getResult() != null) {
                refresh();
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onDelete() {
        PaymentRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) {
            return;
        }
        if (!Dialogs.confirm(owner(), "Удаление платежа",
                "Удалить платёж №" + row.payment().getId() + " на сумму " + FxUtil.money(row.payment().getAmount())
                        + "?\nДействие нельзя отменить.", "Удалить")) {
            return;
        }
        try {
            ctx.paymentService.delete(row.payment().getId());
            refresh();
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onRefresh() {
        refresh();
    }
}
