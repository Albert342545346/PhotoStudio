package ru.mirea.photostudio.fx.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.Dialogs;
import ru.mirea.photostudio.fx.util.FxUtil;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.service.BookingView;

import java.util.List;

/** Вкладка «Клиенты»: таблица, поиск, CRUD и список бронирований выбранного клиента (связанные данные). */
public class ClientsController extends TabController {
    private final AppContext ctx = AppContext.get();

    @FXML
    private TextField searchField;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private TableView<Client> table;
    @FXML
    private TableColumn<Client, String> idColumn;
    @FXML
    private TableColumn<Client, String> nameColumn;
    @FXML
    private TableColumn<Client, String> phoneColumn;
    @FXML
    private TableColumn<Client, String> emailColumn;
    @FXML
    private TableColumn<Client, String> vipColumn;
    @FXML
    private TableColumn<Client, String> registeredColumn;
    @FXML
    private ListView<String> bookingsList;
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
        vipColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isVip() ? "VIP" : ""));
        registeredColumn.setCellValueFactory(c -> new SimpleStringProperty(FxUtil.dateTime(c.getValue().getRegisteredAt())));
        table.setPlaceholder(new Label("Клиенты не найдены"));
        table.setRowFactory(tv -> {
            TableRow<Client> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });
        table.getSelectionModel().selectedItemProperty().addListener((obs, a, b) -> showBookings(b));
        editButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        searchField.textProperty().addListener((obs, a, b) -> refresh());
        refresh();
    }

    @Override
    public void refresh() {
        try {
            Client selected = table.getSelectionModel().getSelectedItem();
            long selectedId = selected == null ? -1 : selected.getId();
            String query = searchField.getText();
            List<Client> clients = query == null || query.isBlank()
                    ? ctx.clientService.getAll() : ctx.clientService.search(query);
            table.setItems(FXCollections.observableArrayList(clients));
            countLabel.setText("Клиентов: " + clients.size());
            clients.stream().filter(c -> c.getId() == selectedId).findFirst()
                    .ifPresent(c -> table.getSelectionModel().select(c));
            showBookings(table.getSelectionModel().getSelectedItem());
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    private void showBookings(Client client) {
        bookingsList.getItems().clear();
        if (client == null) {
            bookingsList.getItems().add("Выберите клиента в таблице, чтобы увидеть его бронирования");
            return;
        }
        try {
            List<BookingView> views = ctx.bookingService.toViews(ctx.bookingService.getByClientId(client.getId()));
            for (BookingView v : views) {
                bookingsList.getItems().add("№" + v.booking().getId() + " · " + FxUtil.dateTime(v.booking().getStartTime())
                        + " · " + v.booking().getSessionType().getTitle() + " · " + v.booking().getStatus().getTitle()
                        + " · " + FxUtil.money(v.booking().getTotalPrice()));
            }
            if (views.isEmpty()) {
                bookingsList.getItems().add("У клиента пока нет бронирований");
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onAdd() {
        open(null, "Новый клиент");
    }

    @FXML
    private void onEdit() {
        Client client = table.getSelectionModel().getSelectedItem();
        if (client != null) {
            open(client, "Клиент №" + client.getId());
        }
    }

    private void open(Client client, String title) {
        try {
            ClientDialogController dialog = Dialogs.openModal(owner(), "/fxml/client-dialog.fxml", title,
                    (ClientDialogController d) -> d.setClient(client));
            if (dialog.getResult() != null) {
                refresh();
            }
        } catch (RuntimeException e) {
            handle(e);
        }
    }

    @FXML
    private void onDelete() {
        Client client = table.getSelectionModel().getSelectedItem();
        if (client == null) {
            return;
        }
        if (!Dialogs.confirm(owner(), "Удаление клиента",
                "Удалить клиента «" + client.getFullName() + "»?\nДействие нельзя отменить.", "Удалить")) {
            return;
        }
        try {
            ctx.clientService.delete(client.getId());
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
