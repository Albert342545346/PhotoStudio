package ru.mirea.photostudio;

import ru.mirea.photostudio.exception.AppException;
import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.DatabaseViewRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.service.BookingService;
import ru.mirea.photostudio.service.ClientService;
import ru.mirea.photostudio.service.DatabaseViewService;
import ru.mirea.photostudio.service.ExportService;
import ru.mirea.photostudio.service.HallService;
import ru.mirea.photostudio.service.StatisticsService;
import ru.mirea.photostudio.service.pricing.LongSessionPricing;
import ru.mirea.photostudio.service.pricing.PricingStrategy;
import ru.mirea.photostudio.service.pricing.StandardPricing;
import ru.mirea.photostudio.service.pricing.WeekendPricing;
import ru.mirea.photostudio.ui.BookingMenu;
import ru.mirea.photostudio.ui.ClientMenu;
import ru.mirea.photostudio.ui.ConsoleInput;
import ru.mirea.photostudio.ui.ExportMenu;
import ru.mirea.photostudio.ui.FilterMenu;
import ru.mirea.photostudio.ui.MainMenu;
import ru.mirea.photostudio.ui.SearchMenu;
import ru.mirea.photostudio.ui.SortMenu;
import ru.mirea.photostudio.util.CsvExporter;
import ru.mirea.photostudio.util.DatabaseManager;
import ru.mirea.photostudio.util.ExcelExporter;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Точка входа. Класс Main только «собирает» приложение из слоёв и запускает главное меню:
 * Console UI -> Service -> Repository/JDBC -> PostgreSQL.
 * Никакой бизнес-логики и SQL здесь нет.
 */
public class Main {

    public static void main(String[] args) {
        // русские буквы в консоли; log4j (библиотека POI) не должен печатать служебные предупреждения
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));
        System.setProperty("log4j2.loggerContextFactory",
                "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");

        try {
            DatabaseManager.checkConnection();
        } catch (AppException e) {
            System.out.println("Ошибка: " + e.getMessage());
            System.out.println("Проверьте: 1) запущен ли PostgreSQL; 2) создана ли база photostudio "
                    + "и выполнен ли скрипт sql/photostudio.sql; 3) верны ли url/user/password в db.properties.");
            return;
        }

        // ---- слой Repository
        ClientRepository clientRepository = new ClientRepository();
        HallRepository hallRepository = new HallRepository();
        BookingRepository bookingRepository = new BookingRepository();
        DatabaseViewRepository databaseViewRepository = new DatabaseViewRepository();

        // ---- стратегии расчёта цены (порядок важен: берётся первая подходящая)
        List<PricingStrategy> pricingStrategies =
                List.of(new WeekendPricing(), new LongSessionPricing(), new StandardPricing());

        // ---- слой Service
        ClientService clientService = new ClientService(clientRepository, bookingRepository);
        HallService hallService = new HallService(hallRepository);
        BookingService bookingService =
                new BookingService(bookingRepository, clientRepository, hallRepository, pricingStrategies);
        StatisticsService statisticsService =
                new StatisticsService(bookingRepository, clientRepository, hallRepository);
        ExportService exportService = new ExportService(clientRepository, hallRepository, bookingRepository,
                new ExcelExporter(), new CsvExporter());
        DatabaseViewService databaseViewService = new DatabaseViewService(databaseViewRepository);

        // ---- слой Console UI
        ConsoleInput input = new ConsoleInput(System.in);
        MainMenu mainMenu = new MainMenu(input,
                new ClientMenu(input, clientService),
                new BookingMenu(input, bookingService, clientService, hallService),
                new SearchMenu(input, bookingService),
                new FilterMenu(input, bookingService, hallService),
                new SortMenu(input, bookingService),
                new ExportMenu(input, exportService),
                hallService, statisticsService, databaseViewService);

        mainMenu.run();
        System.out.println("До свидания!");
    }
}
