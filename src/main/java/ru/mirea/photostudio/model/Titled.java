package ru.mirea.photostudio.model;

/**
 * Интерфейс для перечислений (enum), у которых есть человекочитаемое название на русском языке.
 * Позволяет написать один универсальный метод выбора значения из списка (ConsoleInput.readEnum).
 */
public interface Titled {
    String getTitle();
}
