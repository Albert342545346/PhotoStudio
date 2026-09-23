package ru.mirea.photostudio.service;

import ru.mirea.photostudio.repository.BookingRepository;
import ru.mirea.photostudio.repository.ClientRepository;
import ru.mirea.photostudio.repository.HallRepository;
import ru.mirea.photostudio.util.CsvExporter;
import ru.mirea.photostudio.util.ExcelExporter;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Берёт данные из БД (через репозитории) и передаёт их экспортёрам. Файлы создаются в папке export/. */
public class ExportService {

    private static final Path EXPORT_DIR = Path.of("export");
    private static final DateTimeFormatter FILE_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final ClientRepository clientRepository;
    private final HallRepository hallRepository;
    private final BookingRepository bookingRepository;
    private final ExcelExporter excelExporter;
    private final CsvExporter csvExporter;

    public ExportService(ClientRepository clientRepository, HallRepository hallRepository,
                         BookingRepository bookingRepository, ExcelExporter excelExporter, CsvExporter csvExporter) {
        this.clientRepository = clientRepository;
        this.hallRepository = hallRepository;
        this.bookingRepository = bookingRepository;
        this.excelExporter = excelExporter;
        this.csvExporter = csvExporter;
    }

    public Path exportToExcel() {
        Path file = EXPORT_DIR.resolve("photostudio_" + timestamp() + ".xlsx");
        return excelExporter.export(clientRepository.findAll(), hallRepository.findAll(),
                bookingRepository.findAll(), file);
    }

    public Path exportBookingsToCsv() {
        Path file = EXPORT_DIR.resolve("bookings_" + timestamp() + ".csv");
        return csvExporter.exportBookings(bookingRepository.findAll(), file);
    }

    private String timestamp() {
        return LocalDateTime.now().format(FILE_STAMP);
    }
}
