package ru.mirea.photostudio.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.photostudio.AppContext;
import ru.mirea.photostudio.exception.PhotostudioException;
import ru.mirea.photostudio.fx.util.Dialogs;

import java.io.IOException;

/**
 * КР 2: JavaFX-приложение фотостудии. Работает с той же БД PostgreSQL, что и консольное приложение из КР 1,
 * напрямую через JDBC (слои View/FXML → Controller → Service → Repository → Database).
 */
public class PhotostudioFxApplication extends Application {
    private static final Logger LOG = LoggerFactory.getLogger(PhotostudioFxApplication.class);

    @Override
    public void start(Stage stage) {
        try {
            AppContext.init();
        } catch (PhotostudioException e) {
            showStartupError("Не удалось подключиться к базе данных",
                    e.getMessage() + "\n\nПроверьте, что PostgreSQL запущен, база photostudio создана, "
                            + "а параметры в database.properties (или DB_URL / DB_USER / DB_PASSWORD) верны.");
            Platform.exit();
            return;
        }
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/main-view.fxml"));
            Scene scene = new Scene(root, 1280, 780);
            scene.getStylesheets().add(Dialogs.stylesheet());
            stage.setTitle("Фотостудия — управление бронированиями");
            stage.setMinWidth(1000);
            stage.setMinHeight(640);
            stage.setScene(scene);
            stage.show();
        } catch (IOException | RuntimeException e) {
            LOG.error("Ошибка запуска интерфейса", e);
            showStartupError("Ошибка запуска приложения", "Не удалось открыть главное окно. Подробности записаны в журнал.");
            Platform.exit();
        }
    }

    private void showStartupError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Фотостудия");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
