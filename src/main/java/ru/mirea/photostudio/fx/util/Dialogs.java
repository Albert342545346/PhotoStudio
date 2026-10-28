package ru.mirea.photostudio.fx.util;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.fxml.FXMLLoader;
import ru.mirea.photostudio.fx.controller.DialogController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Стандартные диалоги: сообщения, подтверждение, выбор, модальные формы из FXML, выбор файла. */
public final class Dialogs {
    private static final String CSS = Dialogs.class.getResource("/css/styles.css").toExternalForm();

    private Dialogs() {
    }

    public static String stylesheet() {
        return CSS;
    }

    private static Alert alert(Window owner, Alert.AlertType type, String title, String header, String message) {
        Alert alert = new Alert(type);
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(CSS);
        return alert;
    }

    public static void info(Window owner, String header, String message) {
        alert(owner, Alert.AlertType.INFORMATION, "Сообщение", header, message).showAndWait();
    }

    public static void error(Window owner, String message) {
        alert(owner, Alert.AlertType.ERROR, "Ошибка", "Не удалось выполнить операцию", message).showAndWait();
    }

    /** Показывает пользователю понятное сообщение об исключении (без stack trace). */
    public static void handle(Window owner, Throwable e) {
        error(owner, FxUtil.messageOf(e));
    }

    /** Запрос подтверждения. @return true, если пользователь нажал кнопку подтверждения. */
    public static boolean confirm(Window owner, String header, String message, String okText) {
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = alert(owner, Alert.AlertType.CONFIRMATION, "Подтверждение", header, message);
        alert.getButtonTypes().setAll(ok, cancel);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ok;
    }

    /** Диалог выбора одного из вариантов (по кнопке на вариант). Пустой Optional — «Отмена». */
    public static <T> Optional<T> choose(Window owner, String header, String message, List<T> options,
                                         Function<T, String> label) {
        Map<ButtonType, T> buttons = new LinkedHashMap<>();
        for (T option : options) {
            buttons.put(new ButtonType(label.apply(option), ButtonBar.ButtonData.OK_DONE), option);
        }
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = alert(owner, Alert.AlertType.CONFIRMATION, "Выбор", header, message);
        alert.getButtonTypes().setAll(buttons.keySet());
        alert.getButtonTypes().add(cancel);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(buttons.get(result.get()));
    }

    /**
     * Открывает модальную форму из FXML. Перед показом вызывает init(controller),
     * после закрытия возвращает контроллер — результат берётся через controller.getResult().
     */
    public static <C> C openModal(Window owner, String fxml, String title, Consumer<C> init) {
        try {
            FXMLLoader loader = new FXMLLoader(Dialogs.class.getResource(fxml));
            Parent root = loader.load();
            C controller = loader.getController();
            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(title);
            stage.setResizable(false);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(CSS);
            stage.setScene(scene);
            if (controller instanceof DialogController<?> dialog) {
                dialog.setStage(stage);
            }
            init.accept(controller);
            stage.showAndWait();
            return controller;
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось загрузить окно " + fxml, e);
        }
    }

    /**
     * Диалог «Сохранить как…». Путь выбирает пользователь (FileChooser), расширение .xlsx/.csv
     * добавляется по выбранному фильтру, если пользователь его не указал.
     * @return выбранный путь или null, если пользователь отменил выбор
     */
    public static Path chooseExportFile(Window owner, String title, String initialName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        FileChooser.ExtensionFilter xlsx = new FileChooser.ExtensionFilter("Excel (*.xlsx)", "*.xlsx");
        FileChooser.ExtensionFilter csv = new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv");
        chooser.getExtensionFilters().addAll(xlsx, csv);
        chooser.setSelectedExtensionFilter(xlsx);
        chooser.setInitialFileName(initialName);
        File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return null;
        }
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".csv")) {
            return file.toPath();
        }
        String extension = chooser.getSelectedExtensionFilter() == csv ? ".csv" : ".xlsx";
        return Path.of(file.getPath() + extension);
    }
}
