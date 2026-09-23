package ru.mirea.photostudio.ui;

import ru.mirea.photostudio.model.TableData;
import ru.mirea.photostudio.service.DatabaseViewService;
import ru.mirea.photostudio.service.HallService;
import ru.mirea.photostudio.service.StatisticsService;

import java.util.List;
import java.util.Map;

/** Главное меню системы. Подменю запускаются через их метод run(). */
public class MainMenu extends Menu {

    private final ClientMenu clientMenu;
    private final BookingMenu bookingMenu;
    private final SearchMenu searchMenu;
    private final FilterMenu filterMenu;
    private final SortMenu sortMenu;
    private final ExportMenu exportMenu;
    private final HallService hallService;
    private final StatisticsService statisticsService;
    private final DatabaseViewService databaseViewService;

    public MainMenu(ConsoleInput in, ClientMenu clientMenu, BookingMenu bookingMenu, SearchMenu searchMenu,
                    FilterMenu filterMenu, SortMenu sortMenu, ExportMenu exportMenu, HallService hallService,
                    StatisticsService statisticsService, DatabaseViewService databaseViewService) {
        super(in);
        this.clientMenu = clientMenu;
        this.bookingMenu = bookingMenu;
        this.searchMenu = searchMenu;
        this.filterMenu = filterMenu;
        this.sortMenu = sortMenu;
        this.exportMenu = exportMenu;
        this.hallService = hallService;
        this.statisticsService = statisticsService;
        this.databaseViewService = databaseViewService;
    }

    @Override
    protected String getTitle() {
        return "СИСТЕМА УПРАВЛЕНИЯ ФОТОСТУДИЕЙ";
    }

    @Override
    protected List<String> getItems() {
        return List.of("Клиенты", "Бронирования", "Залы студии", "Поиск", "Фильтрация",
                "Сортировка", "Статистика", "Экспорт данных", "Вывести таблицы базы данных");
    }

    @Override
    protected String getExitLabel() {
        return "Выход";
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1 -> clientMenu.run();
            case 2 -> bookingMenu.run();
            case 3 -> printEntities("Залы студии", hallService.getAll());
            case 4 -> searchMenu.run();
            case 5 -> filterMenu.run();
            case 6 -> sortMenu.run();
            case 7 -> showStatistics();
            case 8 -> exportMenu.run();
            case 9 -> showDatabaseTables();
            default -> System.out.println("Неизвестный пункт меню.");
        }
    }

    private void showStatistics() {
        System.out.println();
        System.out.println("--- Статистика фотостудии ---");
        for (Map.Entry<String, String> entry : statisticsService.collect().entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private void showDatabaseTables() {
        for (TableData table : databaseViewService.readAllTables()) {
            TablePrinter.print(table);
        }
    }
}
