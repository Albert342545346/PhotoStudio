package ru.mirea.photostudio.fx.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Photographer;
import ru.mirea.photostudio.model.SessionType;

import java.math.BigDecimal;

/** Форма фотографа (создание и редактирование). */
public class PhotographerDialogController extends DialogController<Photographer> {
    private final AppContext ctx = AppContext.get();

    @FXML
    private Label titleLabel;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField phoneField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<SessionType> specializationCombo;
    @FXML
    private TextField rateField;
    @FXML
    private CheckBox activeCheck;
    @FXML
    private Label errorLabel;

    private Photographer editing;

    @FXML
    private void initialize() {
        FxUtil.configureCombo(specializationCombo, SessionType::getTitle, "Выберите специализацию");
        specializationCombo.setItems(FXCollections.observableArrayList(SessionType.values()));
        activeCheck.setSelected(true);
        FxUtil.clearFormError(errorLabel);
    }

    /** null — новый фотограф. */
    public void setPhotographer(Photographer photographer) {
        this.editing = photographer;
        if (photographer == null) {
            titleLabel.setText("Новый фотограф");
            return;
        }
        titleLabel.setText("Редактирование фотографа №" + photographer.getId());
        fullNameField.setText(photographer.getFullName());
        phoneField.setText(photographer.getPhone());
        emailField.setText(photographer.getEmail());
        specializationCombo.setValue(photographer.getSpecialization());
        rateField.setText(photographer.getHourlyRate().toPlainString());
        activeCheck.setSelected(photographer.isActive());
    }

    @FXML
    private void onSave() {
        try {
            FxUtil.clearFormError(errorLabel);
            String name = FxUtil.requireText(fullNameField.getText(), "ФИО");
            String phone = FxUtil.requireText(phoneField.getText(), "Телефон");
            String email = FxUtil.requireText(emailField.getText(), "Email");
            SessionType type = FxUtil.requireSelected(specializationCombo.getValue(), "Специализация");
            BigDecimal rate = FxUtil.parseDecimal(rateField.getText(), "Ставка за час");
            boolean active = activeCheck.isSelected();
            Photographer saved;
            if (editing == null) {
                saved = ctx.photographerService.create(new Photographer(name, phone, email, type, rate, active));
            } else {
                saved = ctx.photographerService.update(new Photographer(editing.getId(), name, phone, email, type,
                        rate, active));
            }
            finish(saved);
        } catch (RuntimeException e) {
            FxUtil.showFormError(errorLabel, e);
        }
    }
}
