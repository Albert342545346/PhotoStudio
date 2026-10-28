package ru.mirea.photostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Hall;

import java.math.BigDecimal;

/** Форма зала (создание и редактирование). */
public class HallDialogController extends DialogController<Hall> {
    private final AppContext ctx = AppContext.get();

    @FXML
    private Label titleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextField capacityField;
    @FXML
    private TextField priceField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Label errorLabel;

    private Hall editing;

    @FXML
    private void initialize() {
        FxUtil.clearFormError(errorLabel);
    }

    /** null — новый зал. */
    public void setHall(Hall hall) {
        this.editing = hall;
        if (hall == null) {
            titleLabel.setText("Новый зал");
            return;
        }
        titleLabel.setText("Редактирование зала №" + hall.getId());
        nameField.setText(hall.getName());
        capacityField.setText(String.valueOf(hall.getCapacity()));
        priceField.setText(hall.getHourlyPrice().toPlainString());
        descriptionArea.setText(hall.getDescription() == null ? "" : hall.getDescription());
    }

    @FXML
    private void onSave() {
        try {
            FxUtil.clearFormError(errorLabel);
            String name = FxUtil.requireText(nameField.getText(), "Название");
            int capacity = FxUtil.parseInt(capacityField.getText(), "Вместимость");
            BigDecimal price = FxUtil.parseDecimal(priceField.getText(), "Цена за час");
            String description = FxUtil.optionalText(descriptionArea.getText());
            Hall saved;
            if (editing == null) {
                saved = ctx.hallService.create(new Hall(name, capacity, price, description));
            } else {
                saved = ctx.hallService.update(new Hall(editing.getId(), name, capacity, price, description));
            }
            finish(saved);
        } catch (RuntimeException e) {
            FxUtil.showFormError(errorLabel, e);
        }
    }
}
