package ru.mirea.photostudio.model;

import java.util.List;

/** Содержимое таблицы БД для вывода в консоль (название, колонки, строки). */
public record TableData(String name, List<String> columns, List<List<String>> rows) {
}
