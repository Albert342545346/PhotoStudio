package ru.mirea.photostudio.fx.controller;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.Dialogs;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.BookingSortField;
import ru.mirea.photostudio.model.BookingStatus;
import ru.mirea.photostudio.model.Payment;
import ru.mirea.photostudio.model.SessionType;
import ru.mirea.photostudio.service.BookingQuery;
import ru.mirea.photostudio.service.BookingView;

import java.nio.file.Path;
import java.util.List;

/** Вкладка «Бронирования»: основная сущность — таблица, CRUD, поиск, фильтры, сортировка, экспорт. */
public class BookingsController extends TabController {
    private final AppContext ctx = AppContext.get();

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<BookingStatus> statusCombo;
    @FXML
    private ComboBox<SessionType> typeCombo;
    @FXML
    private DatePicker fromPicker;
    @FXML
    private DatePicker toPicker;
    @FXML
    private ComboBox<BookingSortField> sortCombo;
    @FXML
    private CheckBox descendingCheck;
    @FXML
    private Button editButton;
    @FXML
    private Button statusButton;
    @FXML
    private Button paymentButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<BookingView> table;
    @FXML
    private TableColumn<BookingView, String> idColumn;
    @FXML
    private TableColumn<BookingView, String> clientColumn;
    @FXML
    private TableColumn<BookingView, String> photographerColumn;
    @FXML
    private TableColumn<BookingView, String> hallColumn;
    @FXML
    private TableColumn<BookingView, String> typeColumn;
    @FXML
    private TableColumn<BookingView, String> statusColumn;
    @FXML
    private TableColumn<BookingView, String> startColumn;
    @FXML
    private TableColumn<BookingView, String> endColumn;
    @FXML
    private TableColumn<BookingView, String> participantsColumn;
    @FXML
    private TableColumn<BookingView, String> priceColumn;
    @FXML
    private TableColumn<BookingView, String> paidColumn;
    @FXML
    private Label countLabel;
    @FXML
    private Label detailClient;
    @FXML
    private Label detailPhotographer;
    @FXML
    private Label detailHall;
    @FXML
    private Label detailType;
    @FXML
    private Label detailStatus;
    @FXML
    private Label detailTime;
    @FXML
    private Label detailParticipants;
    @FXML
    private Label detailPrice;
    @FXML
    private Label detailPaid;
    @FXML
    private Label detailComment;
    @FXML
    private ListView<String> paymentsList;

    /** true, пока программа сама меняет элементы фильтров (чтобы не перезагружать таблицу много раз). */
    private boolean updating;

    @Override
    protected Node anchor() {
        return table;
    }

    @FXML
    private void initialize() {
        FxUtil.configureCombo(statusCombo, BookingStatus::getTitle, "Любой статус");
        statusCombo.setItems(FXCollections.observableArrayList(BookingStatus.values()));
        FxUtil.configureCombo(typeCombo, SessionType::getTitle, "Любой тип");
        typeCombo.setItems(FXCollections.observableArrayList(SessionType.values()));
        FxUtil.configureCombo(sortCombo, BookingSortField::getTitle, "Сортировка");
        sortCombo.setItems(FXCollections.observableArrayList(BookingSortField.values()));
        sortCombo.setValue(BookingSortField.START_TIME);

        idColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().booking().getId())));
        clientColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clientName()));
        photographerColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().photographerName()));
        hallColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().hallName()));
        typeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().booking().getSessionType().getTitle()));
        statusColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().booking().getStatus().getTitle()));
        startColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.dateTime(c.getValue().booking().getStartTime())));
        endColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.dateTime(c.getValue().booking().getEndTime())));
        participantsColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().booking().getParticipants())));
        priceColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.money(c.getValue().booking().getTotalPrice())));
        paidColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.money(c.getValue().paid())));
        statusColumn.setCellFactory(column -> new TableCell<BookingView, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-created", "status-confirmed", "status-completed", "status-cancelled");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                BookingView view = getTableRow() == null ? null : getTableRow().getItem();
                if (view != null) {
                    getStyleClass().add("status-" + view.booking().getStatus().name().toLowerCase());
                }
            }
        });
        table.setPlaceholder(new Label("Нет бронирований по заданным условиям"));
        table.setRowFactory(tv -> {
            TableRow<BookingView> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> showDetails(newValue));

        BooleanBinding noSelection = table.getSelectionModel().selectedItemProperty().isNull();
        editButton.disableProperty().bind(noSelection);
        statusButton.disableProperty().bind(noSelection);
        paymentButton.disableProperty().bind(noSelection);
        deleteButton.disableProperty().bind(noSelection);

        // Фильтры и сортировка применяются сразу при изменении; поиск — по Enter или кнопке «Найти».
        statusCombo.valueProperty().addListener((obs, a, b) -> refresh());
        typeCombo.valueProperty().addListener((obs, a, b) -> refresh());
        fromPicker.valueProperty().addListener((obs, a, b) -> refresh());
        toPicker.valueProperty().addListener((obs, a, b) -> refresh());
        sortCombo.valueProperty().addListener((obs, a, b) -> refresh());
        descendingCheck.selectedProperty().addListener((obs, a, b) -> refresh());

        refresh();
    }

    @Override
    public void refresh() {
        if (updating) {
            return;
        }
        try {
            BookingView selected = table.getSelectionModel().getSelectedItem();
            long selectedId = selected == null ? -1 : selected.booking().getId();
            BookingQuery query = new BookingQuery(searchField.getText(), statusCombo.getValue(), typeCombo.getValue(),
                    fromPicker.getValue(), toPicker.getValue(), sortCombo.getValue(), !descendingCheck.isSelected());
            List<BookingView> rows = ctx.bookingService.query(query);
            table.setItems(FXCollections.observableArrayList(rows));
            countLabel.setText("Записей: " + rows.size());
            rows.stream().filter(v -> v.booking().getId() == selectedId).findFirst()
                    .ifPresent(v -> table.getSelectionModel().select(v));
            showDetails(table.getSelectionModel().getSelectedItem());
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    private void showDetails(BookingView view) {
        paymentsList.getItems().clear();
        if (view == null) {
            for (Label label : List.of(detailClient, detailPhotographer, detailHall, detailType, detailStatus,
                    detailTime, detailParticipants, detailPrice, detailPaid, detailComment)) {
                label.setText("—");
            }
            return;
        }
        Booking b = view.booking();
        detailClient.setText(view.clientName());
        detailPhotographer.setText(view.photographerName());
        detailHall.setText(view.hallName());
        detailType.setText(b.getSessionType().getTitle());
        detailStatus.setText(b.getStatus().getTitle());
        detailTime.setText(FxUtil.dateTime(b.getStartTime()) + " — " + FxUtil.dateTime(b.getEndTime()));
        detailParticipants.setText(String.valueOf(b.getParticipants()));
        detailPrice.setText(FxUtil.money(b.getTotalPrice()));
        detailPaid.setText(FxUtil.money(view.paid()) + " (остаток " + FxUtil.money(view.remaining()) + ")");
        detailComment.setText(b.getComment() == null || b.getComment().isBlank() ? "—" : b.getComment());
        try {
            for (Payment p : ctx.paymentService.getByBooking(b.getId())) {
                paymentsList.getItems().add(FxUtil.dateTime(p.getPaidAt()) + " — " + FxUtil.money(p.getAmount())
                        + " (" + p.getMethod().getTitle() + ")");
            }
            if (paymentsList.getItems().isEmpty()) {
                paymentsList.getItems().add("Платежей пока нет");
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    private BookingView selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    @FXML
    private void onApplyFilters() {
        refresh();
    }

    @FXML
    private void onReset() {
        updating = true;
        searchField.clear();
        statusCombo.setValue(null);
        typeCombo.setValue(null);
        fromPicker.setValue(null);
        toPicker.setValue(null);
        sortCombo.setValue(BookingSortField.START_TIME);
        descendingCheck.setSelected(true);
        updating = false;
        refresh();
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onAdd() {
        try {
            BookingDialogController dialog = Dialogs.openModal(owner(), "/fxml/booking-dialog.fxml",
                    "Новое бронирование", (BookingDialogController d) -> d.setBooking(null));
            if (dialog.getResult() != null) {
                refresh();
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onEdit() {
        BookingView view = selected();
        if (view == null) {
            return;
        }
        if (view.booking().getStatus().isFinal()) {
            Dialogs.info(owner(), "Редактирование недоступно",
                    "Бронирование со статусом «" + view.booking().getStatus().getTitle() + "» изменять нельзя.");
            return;
        }
        try {
            BookingDialogController dialog = Dialogs.openModal(owner(), "/fxml/booking-dialog.fxml",
                    "Бронирование №" + view.booking().getId(), (BookingDialogController d) -> d.setBooking(view.booking()));
            if (dialog.getResult() != null) {
                refresh();
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onChangeStatus() {
        BookingView view = selected();
        if (view == null) {
            return;
        }
        Booking b = view.booking();
        List<BookingStatus> options = b.getStatus().nextStatuses();
        if (options.isEmpty()) {
            Dialogs.info(owner(), "Смена статуса недоступна",
                    "Из статуса «" + b.getStatus().getTitle() + "» перейти в другой статус нельзя.");
            return;
        }
        Dialogs.choose(owner(), "Смена статуса бронирования №" + b.getId(),
                "Текущий статус: «" + b.getStatus().getTitle() + "». Выберите новый статус:",
                options, BookingStatus::getTitle).ifPresent(next -> {
            try {
                ctx.bookingService.changeStatus(b.getId(), next);
                refresh();
            } catch (RuntimeException e) {
                handle(e);
            }
        });
    }

    @FXML
    private void onAddPayment() {
        BookingView view = selected();
        if (view == null) {
            return;
        }
        try {
            long bookingId = view.booking().getId();
            PaymentDialogController dialog = Dialogs.openModal(owner(), "/fxml/payment-dialog.fxml",
                    "Приём платежа", (PaymentDialogController d) -> d.setPreselectedBookingId(bookingId));
            if (dialog.getResult() != null) {
                refresh();
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onDelete() {
        BookingView view = selected();
        if (view == null) {
            return;
        }
        boolean confirmed = Dialogs.confirm(owner(), "Удаление бронирования",
                "Удалить бронирование №" + view.booking().getId() + " (клиент: " + view.clientName()
                        + ")?\nДействие нельзя отменить.", "Удалить");
        if (!confirmed) {
            return;
        }
        try {
            ctx.bookingService.delete(view.booking().getId());
            refresh();
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onExport() {
        try {
            if (table.getItems().isEmpty()) {
                Dialogs.info(owner(), "Нет данных", "В таблице нет бронирований для экспорта.");
                return;
            }
            Path path = Dialogs.chooseExportFile(owner(), "Экспорт бронирований", "bookings.xlsx");
            if (path == null) {
                return;
            }
            List<Booking> bookings = table.getItems().stream().map(BookingView::booking).toList();
            Path saved = ctx.exportService.exportBookings(bookings, path);
            Dialogs.info(owner(), "Экспорт выполнен",
                    "Экспортировано записей: " + bookings.size() + "\nФайл: " + saved);
        } catch (RuntimeException e) {
            handle(e);
        }
    }
}
