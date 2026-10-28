package ru.mirea.photostudio;

import ru.mirea.photostudio.repository.jdbc.JdbcBookingRepository;
import ru.mirea.photostudio.repository.jdbc.JdbcClientRepository;
import ru.mirea.photostudio.repository.jdbc.JdbcHallRepository;
import ru.mirea.photostudio.repository.jdbc.JdbcPaymentRepository;
import ru.mirea.photostudio.repository.jdbc.JdbcPhotographerRepository;
import ru.mirea.photostudio.service.BookingService;
import ru.mirea.photostudio.service.ClientService;
import ru.mirea.photostudio.service.ExportService;
import ru.mirea.photostudio.service.HallService;
import ru.mirea.photostudio.service.PaymentService;
import ru.mirea.photostudio.service.PhotographerService;
import ru.mirea.photostudio.service.StatisticsService;
import ru.mirea.photostudio.service.pricing.PricingStrategy;
import ru.mirea.photostudio.service.pricing.StandardPricing;
import ru.mirea.photostudio.service.pricing.VipDiscountPricing;
import ru.mirea.photostudio.service.pricing.WeekendPricing;
import ru.mirea.photostudio.util.DatabaseManager;
import ru.mirea.photostudio.util.ExcelExporter;
import ru.mirea.photostudio.util.TablePrinter;

/**
 * Единая точка сборки слоёв Repository → Service. Её используют и консольное приложение (КР 1),
 * и JavaFX-приложение (КР 2): оба работают с одной и той же базой данных и одним кодом бизнес-логики.
 */
public final class AppContext {
    static {
        // Apache POI пишет логи через log4j-api; без реализации он ругается в консоль — подключаем простую.
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");
    }

    private static AppContext instance;

    public final DatabaseManager db;
    public final ClientService clientService;
    public final PhotographerService photographerService;
    public final HallService hallService;
    public final BookingService bookingService;
    public final PaymentService paymentService;
    public final StatisticsService statisticsService;
    public final ExportService exportService;
    public final TablePrinter tablePrinter;
    private final boolean schemaCreated;

    private AppContext() {
        this.db = new DatabaseManager();
        this.schemaCreated = db.initializeIfNeeded();

        JdbcClientRepository clientRepo = new JdbcClientRepository(db);
        JdbcPhotographerRepository photographerRepo = new JdbcPhotographerRepository(db);
        JdbcHallRepository hallRepo = new JdbcHallRepository(db);
        JdbcBookingRepository bookingRepo = new JdbcBookingRepository(db);
        JdbcPaymentRepository paymentRepo = new JdbcPaymentRepository(db);

        PricingStrategy pricing = new VipDiscountPricing(new WeekendPricing(new StandardPricing()));
        this.clientService = new ClientService(clientRepo, bookingRepo);
        this.photographerService = new PhotographerService(photographerRepo, bookingRepo);
        this.hallService = new HallService(hallRepo, bookingRepo);
        this.bookingService = new BookingService(bookingRepo, clientRepo, photographerRepo, hallRepo,
                paymentRepo, pricing);
        this.paymentService = new PaymentService(paymentRepo, bookingRepo);
        this.statisticsService = new StatisticsService(clientRepo, photographerRepo, hallRepo, bookingRepo, paymentRepo);
        this.exportService = new ExportService(clientRepo, photographerRepo, hallRepo, bookingRepo, paymentRepo,
                new ExcelExporter());
        this.tablePrinter = new TablePrinter(db);
    }

    /** Создаёт контекст при первом вызове (подключается к БД, при необходимости создаёт таблицы). */
    public static synchronized AppContext init() {
        if (instance == null) {
            instance = new AppContext();
        }
        return instance;
    }

    public static synchronized AppContext get() {
        if (instance == null) {
            throw new IllegalStateException("AppContext не инициализирован: вызовите AppContext.init()");
        }
        return instance;
    }

    /** true, если при запуске таблицы были созданы и заполнены тестовыми данными. */
    public boolean isSchemaCreated() {
        return schemaCreated;
    }
}
