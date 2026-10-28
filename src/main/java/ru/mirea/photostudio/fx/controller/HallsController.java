package ru.mirea.photostudio.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.Dialogs;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Hall;

import java.util.List;

/** Вкладка «Залы»: таблица, поиск по названию, CRUD. */
public class HallsController extends TabController {
    private final AppContext ctx = AppContext.get();

    @FXML
    private TextField searchField;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<Hall> table;
    @FXML
    private TableColumn<Hall, String> idColumn;
    @FXML
    private TableColumn<Hall, String> nameColumn;
    @FXML
    private TableColumn<Hall, String> capacityColumn;
    @FXML
    private TableColumn<Hall, String> priceColumn;
    @FXML
    private TableColumn<Hall, String> descriptionColumn;
    @FXML
    private Label countLabel;

    @Override
    protected Node anchor() {
        return table;
    }

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getId())));
        nameColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getName()));
        capacityColumn.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getCapacity())));
        priceColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.money(c.getValue().getHourlyPrice())));
        descriptionColumn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDescription() == null ? "" : c.getValue().getDescription()));
        table.setPlaceholder(new Label("Залы не найдены"));
        table.setRowFactory(tv -> {
            TableRow<Hall> row = new TableRow<>();
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
        refresh();
    }

    @Override
    public void refresh() {
        try {
            Hall selected = table.getSelectionModel().getSelectedItem();
            long selectedId = selected == null ? -1 : selected.getId();
            String query = searchField.getText();
            List<Hall> list = query == null || query.isBlank()
                    ? ctx.hallService.getAll() : ctx.hallService.search(query);
            table.setItems(FXCollections.observableArrayList(list));
            countLabel.setText("Залов: " + list.size());
            list.stream().filter(h -> h.getId() == selectedId).findFirst()
                    .ifPresent(h -> table.getSelectionModel().select(h));
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onAdd() {
        open(null, "Новый зал");
    }

    @FXML
    private void onEdit() {
        Hall hall = table.getSelectionModel().getSelectedItem();
        if (hall != null) {
            open(hall, "Зал №" + hall.getId());
        }
    }

    private void open(Hall hall, String title) {
        try {
            HallDialogController dialog = Dialogs.openModal(owner(), "/fxml/hall-dialog.fxml", title,
                    (HallDialogController d) -> d.setHall(hall));
            if (dialog.getResult() != null) {
                refresh();
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onDelete() {
        Hall hall = table.getSelectionModel().getSelectedItem();
        if (hall == null) {
            return;
        }
        if (!Dialogs.confirm(owner(), "Удаление зала",
                "Удалить зал «" + hall.getName() + "»?\nДействие нельзя отменить.", "Удалить")) {
            return;
        }
        try {
            ctx.hallService.delete(hall.getId());
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
