package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.service.ExportService;

import java.nio.file.Path;
import java.util.List;

/** Меню «Экспорт данных». */
public class ExportMenu extends Menu {

    private final ExportService exportService;

    public ExportMenu(ConsoleInput in, ExportService exportService) {
        super(in);
        this.exportService = exportService;
    }

    @Override
    protected String getTitle() {
        return "ЭКСПОРТ ДАННЫХ";
    }

    @Override
    protected List<String> getItems() {
        return List.of("Экспорт всех данных в Excel (.xlsx)", "Экспорт бронирований в CSV (.csv)");
    }

    @Override
    protected void handle(int choice) {
        Path file = switch (choice) {
            case 1 -> exportService.exportToExcel();
            case 2 -> exportService.exportBookingsToCsv();
            default -> null;
        };
        if (file == null) {
            System.out.println("Неизвестный пункт меню.");
            return;
        }
        System.out.println("Файл сохранён: " + file.toAbsolutePath());
    }
}
