package ru.mirea.photostudio.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.Hall;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.model.SessionType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Форма создания и редактирования бронирования. Связанные объекты выбираются из списков (ComboBox). */
public class BookingDialogController extends DialogController<Booking> {
    private final AppContext ctx = AppContext.get();

    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<Client> clientCombo;
    @FXML
    private ComboBox<Photographer> photographerCombo;
    @FXML
    private ComboBox<Hall> hallCombo;
    @FXML
    private ComboBox<SessionType> typeCombo;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<LocalTime> startCombo;
    @FXML
    private ComboBox<LocalTime> endCombo;
    @FXML
    private Spinner<Integer> participantsSpinner;
    @FXML
    private TextArea commentArea;
    @FXML
    private Label statusLabel;
    @FXML
    private Label statusCaption;
    @FXML
    private Label priceLabel;
    @FXML
    private Label errorLabel;

    private Booking editing;
    private boolean loading;

    @FXML
    private void initialize() {
        FxUtil.configureCombo(clientCombo, c -> c.getFullName() + " · " + c.getPhone(), "Выберите клиента");
        clientCombo.setItems(FXCollections.observableArrayList(ctx.clientService.getAll()));

        FxUtil.configureCombo(photographerCombo, p -> p.getFullName() + " · " + p.getSpecialization().getTitle()
                + " · " + FxUtil.money(p.getHourlyRate()) + "/ч", "Выберите фотографа");
        List<Photographer> active = new ArrayList<>();
        for (Photographer p : ctx.photographerService.getAll()) {
            if (p.isActive()) {
                active.add(p);
            }
        }
        photographerCombo.setItems(FXCollections.observableArrayList(active));

        FxUtil.configureCombo(hallCombo, h -> h.getName() + " · до " + h.getCapacity() + " чел. · "
                + FxUtil.money(h.getHourlyPrice()) + "/ч", "Выберите зал");
        hallCombo.setItems(FXCollections.observableArrayList(ctx.hallService.getAll()));

        FxUtil.configureCombo(typeCombo, SessionType::getTitle, "Выберите тип съёмки");
        typeCombo.setItems(FXCollections.observableArrayList(SessionType.values()));

        // Время — шаг 30 минут в рабочие часы студии 09:00–21:00.
        List<LocalTime> starts = new ArrayList<>();
        List<LocalTime> ends = new ArrayList<>();
        for (LocalTime t = LocalTime.of(9, 0); t.isBefore(LocalTime.of(21, 0)); t = t.plusMinutes(30)) {
            starts.add(t);
            ends.add(t.plusMinutes(30));
        }
        FxUtil.configureCombo(startCombo, LocalTime::toString, "чч:мм");
        startCombo.setItems(FXCollections.observableArrayList(starts));
        FxUtil.configureCombo(endCombo, LocalTime::toString, "чч:мм");
        endCombo.setItems(FXCollections.observableArrayList(ends));

        participantsSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, 1));
        participantsSpinner.setEditable(true);
        datePicker.setValue(LocalDate.now().plusDays(1));
        FxUtil.clearFormError(errorLabel);
        statusCaption.setVisible(false);
        statusCaption.setManaged(false);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        clientCombo.valueProperty().addListener((o, a, b) -> updatePrice());
        photographerCombo.valueProperty().addListener((o, a, b) -> updatePrice());
        hallCombo.valueProperty().addListener((o, a, b) -> updatePrice());
        typeCombo.valueProperty().addListener((o, a, b) -> updatePrice());
        datePicker.valueProperty().addListener((o, a, b) -> updatePrice());
        startCombo.valueProperty().addListener((o, a, b) -> updatePrice());
        endCombo.valueProperty().addListener((o, a, b) -> updatePrice());
        updatePrice();
    }

    /** null — создание нового бронирования, иначе — редактирование переданного. */
    public void setBooking(Booking booking) {
        this.editing = booking;
        if (booking == null) {
            titleLabel.setText("Новое бронирование");
            datePicker.setDayCellFactory(picker -> new DateCell() {
                @Override
                public void updateItem(LocalDate item, boolean empty) {
                    super.updateItem(item, empty);
                    setDisable(empty || item.isBefore(LocalDate.now()));
                }
            });
            return;
        }
        loading = true;
        titleLabel.setText("Редактирование бронирования №" + booking.getId());
        select(clientCombo, c -> c.getId() == booking.getClientId());
        clientCombo.setDisable(true); // клиента у существующего бронирования менять нельзя
        if (photographerCombo.getItems().stream().noneMatch(p -> p.getId() == booking.getPhotographerId())) {
            photographerCombo.getItems().add(ctx.photographerService.getById(booking.getPhotographerId()));
        }
        select(photographerCombo, p -> p.getId() == booking.getPhotographerId());
        select(hallCombo, h -> h.getId() == booking.getHallId());
        typeCombo.setValue(booking.getSessionType());
        datePicker.setValue(booking.getStartTime().toLocalDate());
        startCombo.setValue(booking.getStartTime().toLocalTime());
        endCombo.setValue(booking.getEndTime().toLocalTime());
        participantsSpinner.getValueFactory().setValue(booking.getParticipants());
        commentArea.setText(booking.getComment() == null ? "" : booking.getComment());
        statusCaption.setVisible(true);
        statusCaption.setManaged(true);
        statusLabel.setText(booking.getStatus().getTitle() + " (меняется кнопкой «Статус…» в таблице)");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
        loading = false;
        updatePrice();
    }

    private <T> void select(ComboBox<T> combo, Predicate<T> match) {
        combo.getItems().stream().filter(match).findFirst().ifPresent(combo::setValue);
    }

    private void updatePrice() {
        if (loading) {
            return;
        }
        try {
            priceLabel.setText(FxUtil.money(ctx.bookingService.estimatePrice(buildBooking())));
        } catch (RuntimeException e) {
            priceLabel.setText("— (заполните поля формы)");
        }
    }

    /** Собирает бронирование из полей формы, проверяя обязательные поля и формат. */
    private Booking buildBooking() {
        Client client = FxUtil.requireSelected(clientCombo.getValue(), "Клиент");
        Photographer photographer = FxUtil.requireSelected(photographerCombo.getValue(), "Фотограф");
        Hall hall = FxUtil.requireSelected(hallCombo.getValue(), "Зал");
        SessionType type = FxUtil.requireSelected(typeCombo.getValue(), "Тип съёмки");
        LocalDate date = FxUtil.requireSelected(datePicker.getValue(), "Дата");
        LocalTime start = FxUtil.requireSelected(startCombo.getValue(), "Время начала");
        LocalTime end = FxUtil.requireSelected(endCombo.getValue(), "Время окончания");
        int participants = FxUtil.parseInt(participantsSpinner.getEditor().getText(), "Участников");
        String comment = FxUtil.optionalText(commentArea.getText());
        LocalDateTime startTime = LocalDateTime.of(date, start);
        LocalDateTime endTime = LocalDateTime.of(date, end);
        if (editing == null) {
            return new Booking(client.getId(), photographer.getId(), hall.getId(), type, startTime, endTime,
                    participants, comment);
        }
        return new Booking(editing.getId(), client.getId(), photographer.getId(), hall.getId(), type,
                editing.getStatus(), startTime, endTime, participants, editing.getTotalPrice(), comment,
                editing.getCreatedAt());
    }

    @FXML
    private void onSave() {
        try {
            FxUtil.clearFormError(errorLabel);
            Booking draft = buildBooking();
            Booking saved = editing == null ? ctx.bookingService.create(draft) : ctx.bookingService.update(draft);
            finish(saved);
        } catch (RuntimeException e) {
            FxUtil.showFormError(errorLabel, e);
        }
    }
}
