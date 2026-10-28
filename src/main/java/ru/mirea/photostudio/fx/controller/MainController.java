package ru.mirea.photostudio.fx.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TabPane;
import javafx.stage.Window;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.fx.util.Dialogs;

import java.nio.file.Path;
import java.util.List;

/** Главное окно: меню и вкладки. Данные вкладки перезагружаются при её открытии. */
public class MainController {
    @FXML
    private TabPane tabPane;
    // Контроллеры вкладок внедряются автоматически из <fx:include fx:id="имя"> по правилу «имя + Controller».
    @FXML
    private BookingsController bookingsController;
    @FXML
    private ClientsController clientsController;
    @FXML
    private PhotographersController photographersController;
    @FXML
    private HallsController hallsController;
    @FXML
    private PaymentsController paymentsController;
    @FXML
    private StatisticsController statisticsController;

    @FXML
    private void initialize() {
        List<Refreshable> tabs = List.of(bookingsController, clientsController, photographersController,
                hallsController, paymentsController, statisticsController);
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            int index = newIndex.intValue();
            if (index >= 0 && index < tabs.size()) {
                tabs.get(index).refresh();
            }
        });
    }

    private Window window() {
        return tabPane.getScene() == null ? null : tabPane.getScene().getWindow();
    }

    @FXML
    private void onExportAll() {
        try {
            Path path = Dialogs.chooseExportFile(window(), "Экспорт всех таблиц", "photostudio_export.xlsx");
            if (path == null) {
                return;
            }
            if (path.getFileName().toString().toLowerCase().endsWith(".csv")) {
                Path saved = AppContext.get().exportService.exportBookingsToCsv(path);
                Dialogs.info(window(), "Экспорт выполнен",
                        "Формат CSV содержит только бронирования. Файл сохранён:\n" + saved);
            } else {
                Path saved = AppContext.get().exportService.exportToExcel(path);
                Dialogs.info(window(), "Экспорт выполнен", "Все таблицы сохранены в файл:\n" + saved);
            }
        } catch (RuntimeException e) {
            Dialogs.handle(window(), e);
        }
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onAbout() {
        Dialogs.info(window(), "Фотостудия",
                "Контрольные работы 1–2: консольное и JavaFX-приложение для управления фотостудией.\n"
                        + "Данные хранятся в PostgreSQL, доступ — через JDBC.");
    }
}
