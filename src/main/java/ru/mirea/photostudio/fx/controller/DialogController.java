package ru.mirea.photostudio.fx.controller;

import javafx.fxml.FXML;
import javafx.stage.Stage;

/** Базовый контроллер модальной формы: хранит окно и результат (null — пользователь отменил). */
public abstract class DialogController<R> {
    private Stage stage;
    private R result;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public R getResult() {
        return result;
    }

    /** Сохраняет результат и закрывает форму. */
    protected void finish(R value) {
        this.result = value;
        stage.close();
    }

    @FXML
    public void onCancel() {
        stage.close();
    }
}
