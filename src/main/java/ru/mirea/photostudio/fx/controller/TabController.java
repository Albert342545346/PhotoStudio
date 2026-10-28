package ru.mirea.photostudio.fx.controller;

import javafx.scene.Node;
import javafx.stage.Window;
import ru.mirea.photostudio.fx.util.Dialogs;

/** Общая часть контроллеров вкладок: окно-владелец для диалогов и единая обработка ошибок. */
public abstract class TabController implements Refreshable {

    /** Любой элемент вкладки — по нему определяется окно-владелец. */
    protected abstract Node anchor();

    protected Window owner() {
        Node node = anchor();
        return node == null || node.getScene() == null ? null : node.getScene().getWindow();
    }

    protected void handle(Throwable e) {
        Dialogs.handle(owner(), e);
    }
}
