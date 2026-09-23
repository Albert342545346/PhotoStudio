package ru.mirea.photostudio.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.photostudio.exception.ExportException;
import ru.mirea.photostudio.model.Booking;
import ru.mirea.photostudio.model.Client;
import ru.mirea.photostudio.model.Hall;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Экспорт данных в Excel (.xlsx) через Apache POI: три листа — клиенты, залы, бронирования. */
public class ExcelExporter {

    private record Styles(CellStyle header, CellStyle dateTime, CellStyle money) {
    }

    public Path export(List<Client> clients, List<Hall> halls, List<Booking> bookings, Path target) {
        try {
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Workbook workbook = new XSSFWorkbook();
                 OutputStream out = Files.newOutputStream(target)) {
                Styles styles = createStyles(workbook);

                List<Object[]> clientRows = new ArrayList<>();
                for (Client c : clients) {
                    clientRows.add(new Object[]{c.getId(), c.getFullName(), c.getPhone(), c.getEmail(), c.getRegisteredAt()});
                }
                writeSheet(workbook, styles, "Клиенты",
                        new String[]{"ID", "ФИО", "Телефон", "Email", "Дата регистрации"},
                        new int[]{6, 34, 18, 32, 20}, clientRows);

                List<Object[]> hallRows = new ArrayList<>();
                for (Hall h : halls) {
                    hallRows.add(new Object[]{h.getId(), h.getName(), h.getDescription(), h.getCapacity(), h.getPricePerHour()});
                }
                writeSheet(workbook, styles, "Залы",
                        new String[]{"ID", "Название", "Описание", "Вместимость", "Цена за час, ₽"},
                        new int[]{6, 18, 52, 14, 16}, hallRows);

                List<Object[]> bookingRows = new ArrayList<>();
                for (Booking b : bookings) {
                    bookingRows.add(new Object[]{
                            b.getId(), b.getTitle(), b.getClientName(), b.getHallName(),
                            b.getSessionType().getTitle(), b.getStartTime(), b.getEndTime(),
                            b.getDurationHours(), b.getPrice(), b.getStatus().getTitle(), b.getDescription()});
                }
                writeSheet(workbook, styles, "Бронирования",
                        new String[]{"ID", "Название", "Клиент", "Зал", "Тип съёмки", "Начало", "Окончание",
                                "Часов", "Стоимость, ₽", "Статус", "Описание"},
                        new int[]{6, 38, 30, 16, 18, 18, 18, 8, 16, 16, 44}, bookingRows);

                workbook.write(out);
            }
            return target;
        } catch (IOException e) {
            throw new ExportException("Не удалось записать Excel-файл: " + e.getMessage(), e);
        } catch (LinkageError e) {
            // например, NoClassDefFoundError, если Maven не подтянул зависимости Apache POI
            throw new ExportException("Не найдена библиотека Apache POI. Проверьте зависимости в pom.xml "
                    + "(обновите Maven-проект): " + e, e);
        }
    }

    private Styles createStyles(Workbook workbook) {
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());

        CellStyle header = workbook.createCellStyle();
        header.setFont(headerFont);
        header.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        header.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dateTime = workbook.createCellStyle();
        dateTime.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy hh:mm"));

        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("#,##0.00"));

        return new Styles(header, dateTime, money);
    }

    private void writeSheet(Workbook workbook, Styles styles, String name,
                            String[] headers, int[] widths, List<Object[]> rows) {
        Sheet sheet = workbook.createSheet(name);

        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.header());
            sheet.setColumnWidth(i, widths[i] * 256); // ширина задаётся в 1/256 символа
        }

        int rowIndex = 1;
        for (Object[] values : rows) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < values.length; i++) {
                setCell(row.createCell(i), values[i], styles);
            }
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));
    }

    /** Тип значения определяет тип ячейки Excel: число, деньги, дата или текст. */
    private void setCell(Cell cell, Object value, Styles styles) {
        if (value instanceof Integer number) {
            cell.setCellValue(number);
        } else if (value instanceof BigDecimal amount) {
            cell.setCellValue(amount.doubleValue());
            cell.setCellStyle(styles.money());
        } else if (value instanceof LocalDateTime dateTime) {
            cell.setCellValue(Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant()));
            cell.setCellStyle(styles.dateTime());
        } else {
            cell.setCellValue(value == null ? "" : value.toString());
        }
    }
}
