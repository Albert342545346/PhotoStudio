package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.exception.PhotostudioException;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Шаблон консольного меню. После выполнения действия программа возвращается в то же меню.
 * Любая ошибка (ввод, бизнес-правило, БД) перехватывается и выводится пользователю.
 */
public abstract class AbstractMenu {
    protected final ConsoleInput in;
    protected final ConsolePrinter out;

    protected AbstractMenu(ConsoleInput in, ConsolePrinter out) {
        this.in = in;
        this.out = out;
    }

    protected abstract String title();

    protected abstract List<String> items();

    /** @return false, если пункт меню неизвестен */
    protected abstract boolean handle(String choice);

    protected String exitLabel() {
        return "Назад";
    }

    public void run() {
        while (true) {
            printMenu();
            String choice = in.line("Выберите действие: ");
            if (choice.equals("0")) {
                return;
            }
            try {
                if (!handle(choice)) {
                    System.out.println("Ошибка: пункта меню «" + choice + "» не существует.");
                }
            } catch (PhotostudioException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (NoSuchElementException e) {
                throw e; // ввод закрыт — выходим из программы
            } catch (RuntimeException e) {
                System.out.println("Непредвиденная ошибка: " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=".repeat(18) + " " + title() + " " + "=".repeat(18));
        items().forEach(System.out::println);
        System.out.println("0. " + exitLabel());
    }
}
