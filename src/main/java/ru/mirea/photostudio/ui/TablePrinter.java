package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.TableData;

import java.util.List;

/** Печатает таблицу БД в консоль в виде выровненной таблицы. */
public final class TablePrinter {

    private static final int MAX_COLUMN_WIDTH = 28;

    private TablePrinter() {
    }

    public static void print(TableData table) {
        List<String> columns = table.columns();
        int[] widths = new int[columns.size()];
        for (int i = 0; i < widths.length; i++) {
            widths[i] = columns.get(i).length();
        }
        for (List<String> row : table.rows()) {
            for (int i = 0; i < widths.length; i++) {
                widths[i] = Math.max(widths[i], Math.min(row.get(i).length(), MAX_COLUMN_WIDTH));
            }
        }

        System.out.println();
        System.out.println("Таблица: " + table.name() + " (записей: " + table.rows().size() + ")");
        printRow(columns, widths);
        printSeparator(widths);
        for (List<String> row : table.rows()) {
            printRow(row, widths);
        }
    }

    private static void printRow(List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder("| ");
        for (int i = 0; i < widths.length; i++) {
            line.append(String.format("%-" + widths[i] + "s", cut(cells.get(i), widths[i]))).append(" | ");
        }
        System.out.println(line.toString().stripTrailing());
    }

    private static void printSeparator(int[] widths) {
        StringBuilder line = new StringBuilder("|");
        for (int width : widths) {
            line.append("-".repeat(width + 2)).append("|");
        }
        System.out.println(line);
    }

    private static String cut(String text, int width) {
        return text.length() <= width ? text : text.substring(0, width - 1) + "…";
    }
}
