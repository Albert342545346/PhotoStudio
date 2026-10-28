package ru.mirea.photostudio;

import ru.mirea.photostudio.exception.PhotostudioException;
import ru.mirea.photostudio.ui.BookingMenu;
import ru.mirea.photostudio.ui.ClientMenu;
import ru.mirea.photostudio.ui.ConsoleInput;
import ru.mirea.photostudio.ui.ConsolePrinter;
import ru.mirea.photostudio.ui.FilterMenu;
import ru.mirea.photostudio.ui.HallMenu;
import ru.mirea.photostudio.ui.MainMenu;
import ru.mirea.photostudio.ui.PaymentMenu;
import ru.mirea.photostudio.ui.PhotographerMenu;
import ru.mirea.photostudio.ui.SearchMenu;
import ru.mirea.photostudio.ui.SortMenu;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

/**
 * КР 1: консольное приложение. Слои: Console UI -> Service -> Repository / JDBC -> PostgreSQL.
 * (JavaFX-версия из КР 2 запускается классом ru.mirea.photostudio.fx.Launcher.)
 */
public class Main {
    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err), true, StandardCharsets.UTF_8));

        try {
            AppContext ctx = AppContext.init();
            if (ctx.isSchemaCreated()) {
                System.out.println("База данных пуста: созданы таблицы и загружены тестовые данные.");
            }

            ConsoleInput in = new ConsoleInput(System.in);
            ConsolePrinter printer = new ConsolePrinter(ctx.clientService, ctx.photographerService,
                    ctx.hallService, ctx.paymentService);
            MainMenu menu = new MainMenu(in, printer,
                    new ClientMenu(in, printer, ctx.clientService),
                    new PhotographerMenu(in, printer, ctx.photographerService),
                    new HallMenu(in, printer, ctx.hallService),
                    new BookingMenu(in, printer, ctx.bookingService, ctx.clientService,
                            ctx.photographerService, ctx.hallService),
                    new PaymentMenu(in, printer, ctx.paymentService),
                    new SearchMenu(in, printer, ctx.clientService, ctx.photographerService, ctx.bookingService),
                    new FilterMenu(in, printer, ctx.bookingService),
                    new SortMenu(in, printer, ctx.bookingService),
                    ctx.statisticsService, ctx.exportService, ctx.tablePrinter);
            menu.run();
            System.out.println("Работа завершена. До свидания!");
        } catch (PhotostudioException e) {
            System.err.println("Ошибка запуска: " + e.getMessage());
            System.err.println("Проверьте, что PostgreSQL запущен, база photostudio создана, а настройки в "
                    + "src/main/resources/database.properties (или DB_URL/DB_USER/DB_PASSWORD) верны.");
            System.exit(1);
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Ввод завершён. Выход из программы.");
        }
    }
}
