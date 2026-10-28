package ru.mirea.photostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Client;

/** Форма клиента (создание и редактирование). */
public class ClientDialogController extends DialogController<Client> {
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
    private CheckBox vipCheck;
    @FXML
    private Label errorLabel;

    private Client editing;

    @FXML
    private void initialize() {
        FxUtil.clearFormError(errorLabel);
    }

    /** null — новый клиент. */
    public void setClient(Client client) {
        this.editing = client;
        if (client == null) {
            titleLabel.setText("Новый клиент");
            return;
        }
        titleLabel.setText("Редактирование клиента №" + client.getId());
        fullNameField.setText(client.getFullName());
        phoneField.setText(client.getPhone());
        emailField.setText(client.getEmail());
        vipCheck.setSelected(client.isVip());
    }

    @FXML
    private void onSave() {
        try {
            FxUtil.clearFormError(errorLabel);
            String name = FxUtil.requireText(fullNameField.getText(), "ФИО");
            String phone = FxUtil.requireText(phoneField.getText(), "Телефон");
            String email = FxUtil.requireText(emailField.getText(), "Email");
            boolean vip = vipCheck.isSelected();
            Client saved;
            if (editing == null) {
                saved = ctx.clientService.create(new Client(name, phone, email, vip));
            } else {
                saved = ctx.clientService.update(new Client(editing.getId(), name, phone, email, vip,
                        editing.getRegisteredAt()));
            }
            finish(saved);
        } catch (RuntimeException e) {
            FxUtil.showFormError(errorLabel, e);
        }
    }
}
