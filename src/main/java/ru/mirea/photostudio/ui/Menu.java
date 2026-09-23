package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.exception.AppException;
import ru.mirea.photostudio.model.BaseEntity;

import java.util.List;

/**
 * Абстрактное меню (паттерн «Шаблонный метод»).
 * run() — общий цикл: показать пункты, прочитать выбор, вызвать handle(), обработать ошибки.
 * Наследники задают только заголовок, пункты и действия (getTitle, getItems, handle).
 * Программа не завершается после действия: меню повторяется, пока не выбран пункт 0.
 */
public abstract class Menu {

    private static final String LINE = "=".repeat(60);

    protected final ConsoleInput in;

    protected Menu(ConsoleInput in) {
        this.in = in;
    }

    protected abstract String getTitle();

    protected abstract List<String> getItems();

    /** Выполняет действие пункта меню с номером choice (нумерация с 1). */
    protected abstract void handle(int choice);

    protected String getExitLabel() {
        return "Назад";
    }

    public void run() {
        while (true) {
            printMenu();
            int choice;
            try {
                choice = in.readInt("Выберите действие: ", "Номер пункта меню должен быть целым числом.");
            } catch (AppException e) {
                System.out.println("Ошибка: " + e.getMessage());
                continue;
            }
            if (choice == 0) {
                return;
            }
            if (choice < 1 || choice > getItems().size()) {
                System.out.println("Ошибка: пункта меню с номером " + choice + " не существует.");
                continue;
            }
            try {
                handle(choice);
            } catch (AppException e) {
                // BusinessException, EntityNotFoundException, DatabaseException, InvalidInputException...
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Непредвиденная ошибка: " + e);
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println(LINE);
        System.out.println(getTitle());
        System.out.println(LINE);
        List<String> items = getItems();
        for (int i = 0; i < items.size(); i++) {
            System.out.println((i + 1) + ". " + items.get(i));
        }
        System.out.println("0. " + getExitLabel());
    }

    /** Печать списка сущностей. Для каждой вызывается describe() своего класса — полиморфизм. */
    protected void printEntities(String header, List<? extends BaseEntity> entities) {
        System.out.println();
        System.out.println("--- " + header + " ---");
        if (entities.isEmpty()) {
            System.out.println("Ничего не найдено.");
            return;
        }
        for (BaseEntity entity : entities) {
            String text = entity.describe();
            System.out.println(text);
            if (text.contains("\n")) {
                System.out.println(); // пустая строка между многострочными карточками
            }
        }
        System.out.println("Найдено записей: " + entities.size());
    }
}
