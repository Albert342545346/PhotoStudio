package ru.mirea.photostudio.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.photostudio.exception.ExportException;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Экспорт табличных данных в Excel (.xlsx, Apache POI) и CSV. */
public class ExcelExporter {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    /** Данные одного листа: заголовки и строки. */
    public record SheetData(List<String> headers, List<List<Object>> rows) {
    }

    public void exportXlsx(Path file, Map<String, SheetData> sheets) {
        try {
            createParentDirectories(file);
            try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(file)) {
                CellStyle headerStyle = workbook.createCellStyle();
                Font bold = workbook.createFont();
                bold.setBold(true);
                headerStyle.setFont(bold);
                headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                for (Map.Entry<String, SheetData> entry : sheets.entrySet()) {
                    Sheet sheet = workbook.createSheet(entry.getKey());
                    SheetData data = entry.getValue();
                    Row headerRow = sheet.createRow(0);
                    for (int c = 0; c < data.headers().size(); c++) {
                        Cell cell = headerRow.createCell(c);
                        cell.setCellValue(data.headers().get(c));
                        cell.setCellStyle(headerStyle);
                    }
                    for (int r = 0; r < data.rows().size(); r++) {
                        Row row = sheet.createRow(r + 1);
                        List<Object> values = data.rows().get(r);
                        for (int c = 0; c < values.size(); c++) {
                            writeCell(row.createCell(c), values.get(c));
                        }
                    }
                    for (int c = 0; c < data.headers().size(); c++) {
                        sheet.autoSizeColumn(c);
                    }
                }
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new ExportException("Не удалось записать Excel-файл: " + e.getMessage(), e);
        }
    }

    /** CSV с разделителем «;» и BOM, чтобы Excel корректно открыл кириллицу. */
    public void exportCsv(Path file, SheetData data) {
        try {
            createParentDirectories(file);
            StringBuilder sb = new StringBuilder("\uFEFF");
            sb.append(String.join(";", data.headers().stream().map(ExcelExporter::csv).toList())).append("\r\n");
            for (List<Object> row : data.rows()) {
                sb.append(String.join(";", row.stream().map(v -> csv(format(v))).toList())).append("\r\n");
            }
            Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ExportException("Не удалось записать CSV-файл: " + e.getMessage(), e);
        }
    }

    private void writeCell(Cell cell, Object value) {
        if (value instanceof BigDecimal number) {
            cell.setCellValue(number.doubleValue());
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(format(value));
        }
    }

    private static String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dateTime) {
            return DATE_TIME.format(dateTime);
        }
        if (value instanceof Boolean flag) {
            return flag ? "Да" : "Нет";
        }
        return String.valueOf(value);
    }

    private static String csv(String value) {
        String escaped = value.replace("\"", "\"\"");
        return (escaped.contains(";") || escaped.contains("\"") || escaped.contains("\n"))
                ? "\"" + escaped + "\"" : escaped;
    }

    private void createParentDirectories(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
