package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.service.ExportService;
import ru.mirea.photostudio.service.StatisticsService;
import ru.mirea.photostudio.util.TablePrinter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/** Главное меню системы фотостудии. */
public class MainMenu extends AbstractMenu {
    private static final Path EXCEL_FILE = Path.of("export", "photostudio_export.xlsx");
    private static final Path CSV_FILE = Path.of("export", "bookings.csv");

    private final ClientMenu clientMenu;
    private final PhotographerMenu photographerMenu;
    private final HallMenu hallMenu;
    private final BookingMenu bookingMenu;
    private final PaymentMenu paymentMenu;
    private final SearchMenu searchMenu;
    private final FilterMenu filterMenu;
    private final SortMenu sortMenu;
    private final StatisticsService statistics;
    private final ExportService exportService;
    private final TablePrinter tablePrinter;

    public MainMenu(ConsoleInput in, ConsolePrinter out, ClientMenu clientMenu, PhotographerMenu photographerMenu,
                    HallMenu hallMenu, BookingMenu bookingMenu, PaymentMenu paymentMenu, SearchMenu searchMenu,
                    FilterMenu filterMenu, SortMenu sortMenu, StatisticsService statistics,
                    ExportService exportService, TablePrinter tablePrinter) {
        super(in, out);
        this.clientMenu = clientMenu;
        this.photographerMenu = photographerMenu;
        this.hallMenu = hallMenu;
        this.bookingMenu = bookingMenu;
        this.paymentMenu = paymentMenu;
        this.searchMenu = searchMenu;
        this.filterMenu = filterMenu;
        this.sortMenu = sortMenu;
        this.statistics = statistics;
        this.exportService = exportService;
        this.tablePrinter = tablePrinter;
    }

    @Override
    protected String title() {
        return "СИСТЕМА УПРАВЛЕНИЯ ФОТОСТУДИЕЙ";
    }

    @Override
    protected String exitLabel() {
        return "Выход";
    }

    @Override
    protected List<String> items() {
        return List.of("1. Клиенты", "2. Фотографы", "3. Залы", "4. Бронирования", "5. Платежи",
                "6. Поиск", "7. Фильтрация", "8. Сортировка", "9. Статистика", "10. Экспорт данных",
                "11. Вывести таблицы базы данных");
    }

    @Override
    protected boolean handle(String choice) {
        switch (choice) {
            case "1" -> clientMenu.run();
            case "2" -> photographerMenu.run();
            case "3" -> hallMenu.run();
            case "4" -> bookingMenu.run();
            case "5" -> paymentMenu.run();
            case "6" -> searchMenu.run();
            case "7" -> filterMenu.run();
            case "8" -> sortMenu.run();
            case "9" -> showStatistics();
            case "10" -> export();
            case "11" -> TablePrinter.TABLES.forEach(t -> System.out.println(tablePrinter.dump(t)));
            default -> {
                return false;
            }
        }
        return true;
    }

    private void showStatistics() {
        System.out.println("--- Статистика системы ---");
        for (Map.Entry<String, String> entry : statistics.collect().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private void export() {
        System.out.println("1. Excel (.xlsx) — все таблицы");
        System.out.println("2. CSV — бронирования");
        String format = in.line("Выберите формат: ");
        switch (format) {
            case "1" -> System.out.println("Данные экспортированы: " + exportService.exportToExcel(EXCEL_FILE));
            case "2" -> System.out.println("Данные экспортированы: " + exportService.exportBookingsToCsv(CSV_FILE));
            default -> System.out.println("Ошибка: неизвестный формат экспорта.");
        }
    }
}
