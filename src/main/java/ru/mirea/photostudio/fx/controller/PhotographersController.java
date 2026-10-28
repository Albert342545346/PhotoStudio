package ru.mirea.photostudio.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.Dialogs;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Photographer;

import java.util.List;

/** Вкладка «Фотографы»: таблица, поиск, фильтр «только работающие», CRUD. */
public class PhotographersController extends TabController {
    private final AppContext ctx = AppContext.get();

    @FXML
    private TextField searchField;
    @FXML
    private CheckBox activeOnlyCheck;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<Photographer> table;
    @FXML
    private TableColumn<Photographer, String> idColumn;
    @FXML
    private TableColumn<Photographer, String> nameColumn;
    @FXML
    private TableColumn<Photographer, String> phoneColumn;
    @FXML
    private TableColumn<Photographer, String> emailColumn;
    @FXML
    private TableColumn<Photographer, String> specializationColumn;
    @FXML
    private TableColumn<Photographer, String> rateColumn;
    @FXML
    private TableColumn<Photographer, String> activeColumn;
    @FXML
    private Label countLabel;

    @Override
    protected Node anchor() {
        return table;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        phoneColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPhone()));
        emailColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEmail()));
        specializationColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSpecialization().getTitle()));
        rateColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.money(c.getValue().getHourlyRate())));
        activeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Да" : "Нет"));
        table.setPlaceholder(new Label("Фотографы не найдены"));
        table.setRowFactory(tv -> {
            TableRow<Photographer> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });
        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        searchField.textProperty().addListener((obs, a, b) -> refresh());
        activeOnlyCheck.selectedProperty().addListener((obs, a, b) -> refresh());
        refresh();
    }

    @Override
    public void refresh() {
        try {
            Photographer selected = table.getSelectionModel().getSelectedItem();
            long selectedId = selected == null ? -1 : selected.getId();
            String query = searchField.getText();
            List<Photographer> list = query == null || query.isBlank()
                    ? ctx.photographerService.getAll() : ctx.photographerService.search(query);
            if (activeOnlyCheck.isSelected()) {
                list = list.stream().filter(Photographer::isActive).toList();
            }
            table.setItems(FXCollections.observableArrayList(list));
            countLabel.setText("Фотографов: " + list.size());
            list.stream().filter(p -> p.getId() == selectedId).findFirst()
                    .ifPresent(p -> table.getSelectionModel().select(p));
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onAdd() {
        open(null, "Новый фотограф");
    }

    @FXML
    private void onEdit() {
        Photographer p = table.getSelectionModel().getSelectedItem();
        if (p != null) {
            open(p, "Фотограф №" + p.getId());
        }
    }

    private void open(Photographer photographer, String title) {
        try {
            PhotographerDialogController dialog = Dialogs.openModal(owner(), "/fxml/photographer-dialog.fxml", title,
                    (PhotographerDialogController d) -> d.setPhotographer(photographer));
            if (dialog.getResult() != null) {
                refresh();
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onDelete() {
        Photographer p = table.getSelectionModel().getSelectedItem();
        if (p == null) {
            return;
        }
        if (!Dialogs.confirm(owner(), "Удаление фотографа",
                "Удалить фотографа «" + p.getFullName() + "»?\nДействие нельзя отменить.", "Удалить")) {
            return;
        }
        try {
            ctx.photographerService.delete(p.getId());
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
