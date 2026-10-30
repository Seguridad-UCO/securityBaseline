package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * Lector/generador deliberadamente aislado del contrato HTTP. No normaliza valores de celdas.
 */
public final class BulkResourceWorkbookService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    public static final int MAX_ROWS = 1_000;
    private static final List<String> HEADERS = List.of("Aplicación*", "Ruta*", "Método HTTP*");
    private static final List<String> VERBS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS");

    public byte[] template(boolean xlsx) {
        try (Workbook book = xlsx ? new XSSFWorkbook() : new org.apache.poi.hssf.usermodel.HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Recursos");
            CellStyle headerStyle = headerStyle(book);
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i == 1 ? 12_000 : 8_000);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, MAX_ROWS, 0, HEADERS.size() - 1));
            DataValidationHelper helper = sheet.getDataValidationHelper();
            DataValidationConstraint constraint = helper.createExplicitListConstraint(VERBS.toArray(String[]::new));
            DataValidation validation = helper.createValidation(constraint, new CellRangeAddressList(1, MAX_ROWS, 2, 2));
            validation.setSuppressDropDownArrow(false);
            sheet.addValidationData(validation);
            Sheet instructions = book.createSheet("Guía");
            instructions.setColumnWidth(0, 24_000);
            instructions.createRow(0).createCell(0).setCellValue("Carga de recursos");
            instructions.createRow(1).createCell(0).setCellValue("Una fila por endpoint. Los campos con * son obligatorios.");
            instructions.createRow(2).createCell(0).setCellValue("Aplicación: seleccione una aplicación que pertenezca a su tenant.");
            instructions.createRow(3).createCell(0).setCellValue("Ruta: debe comenzar por / y no admite espacios. Máximo 1.000 filas.");
            instructions.createRow(4).createCell(0).setCellValue("Método HTTP: selecciónelo de la lista desplegable.");
            instructions.createRow(5).createCell(0).setCellValue("Ejemplo: Mi aplicación | /api/orders | GET");
            // Alias temporal para libros y automatizaciones que aún abren la hoja histórica.
            Sheet legacyInstructions = book.createSheet("Instrucciones");
            legacyInstructions.createRow(0).createCell(0).setCellValue("Consulte la hoja Guía.");
            book.write(out);
            return out.toByteArray();
        } catch (IOException error) {
            throw new IllegalStateException("No fue posible generar la plantilla", error);
        }
    }

    public ParsedWorkbook parse(byte[] bytes) {
        List<BulkResourceValidationResponse.BulkResourceRowError> errors = new ArrayList<>();
        if (bytes == null || bytes.length == 0)
            return new ParsedWorkbook(List.of(), List.of(error(0, "file", "", "EMPTY_FILE", "El archivo está vacío")));
        if (bytes.length > MAX_BYTES)
            return new ParsedWorkbook(List.of(), List.of(error(0, "file", "", "FILE_TOO_LARGE", "El archivo supera el límite de 5 MB")));
        try (Workbook book = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = book.getSheet("Recursos");
            if (sheet == null)
                return new ParsedWorkbook(List.of(), List.of(error(0, "file", "", "MISSING_SHEET", "Falta la hoja Recursos")));
            if (!headersAreValid(sheet.getRow(0)))
                return new ParsedWorkbook(List.of(), List.of(error(1, "headers", "", "INVALID_HEADERS", "Los encabezados deben ser Aplicación*, Ruta* y Método HTTP*")));
            List<RowValue> rows = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            int last = sheet.getLastRowNum();
            if (last > MAX_ROWS)
                errors.add(error(0, "file", "", "TOO_MANY_ROWS", "El archivo supera las 1.000 filas de datos"));
            for (int index = 1; index <= Math.min(last, MAX_ROWS); index++) {
                Row row = sheet.getRow(index);
                String app = cell(row, 0, index + 1, errors);
                String path = cell(row, 1, index + 1, errors);
                String method = cell(row, 2, index + 1, errors);
                if (app.isEmpty() && path.isEmpty() && method.isEmpty()) continue;
                if (app.isEmpty())
                    errors.add(error(index + 1, "Aplicación*", app, "REQUIRED", "La aplicación es obligatoria"));
                if (path.isEmpty()) errors.add(error(index + 1, "Ruta*", path, "REQUIRED", "La ruta es obligatoria"));
                if (method.isEmpty())
                    errors.add(error(index + 1, "Método HTTP*", method, "REQUIRED", "El método HTTP es obligatorio"));
                String key = app.trim().toLowerCase(Locale.ROOT) + "\u0000" + path + "\u0000" + method.trim().toUpperCase(Locale.ROOT);
                if (!app.isEmpty() && !path.isEmpty() && !method.isEmpty() && !seen.add(key))
                    errors.add(error(index + 1, "row", "", "DUPLICATE_IN_FILE", "El recurso está repetido en el archivo"));
                rows.add(new RowValue(index + 1, app, path, method));
            }
            if (rows.isEmpty()) errors.add(error(0, "file", "", "EMPTY_FILE", "El archivo no contiene filas de datos"));
            return new ParsedWorkbook(rows, errors);
        } catch (EncryptedDocumentException error) {
            return new ParsedWorkbook(List.of(), List.of(error(0, "file", "", "ENCRYPTED_FILE", "No se aceptan libros cifrados")));
        } catch (Exception error) {
            return new ParsedWorkbook(List.of(), List.of(error(0, "file", "", "INVALID_WORKBOOK", "El archivo no es un libro Excel válido")));
        }
    }

    public byte[] annotate(byte[] bytes, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        try (Workbook book = WorkbookFactory.create(new ByteArrayInputStream(bytes)); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.getSheet("Recursos");
            if (sheet == null) throw new IllegalArgumentException("Falta la hoja Recursos");
            sheet.getRow(0).createCell(3).setCellValue("Código de error");
            sheet.getRow(0).createCell(4).setCellValue("Descripción de error");
            Map<Integer, List<BulkResourceValidationResponse.BulkResourceRowError>> byRow = new HashMap<>();
            for (var error : errors)
                if (error.row() > 0) byRow.computeIfAbsent(error.row(), ignored -> new ArrayList<>()).add(error);
            for (var entry : byRow.entrySet()) {
                Row row = sheet.getRow(entry.getKey() - 1);
                if (row == null) row = sheet.createRow(entry.getKey() - 1);
                row.createCell(3).setCellValue(entry.getValue().stream().map(BulkResourceValidationResponse.BulkResourceRowError::code).distinct().reduce((a, b) -> a + ", " + b).orElse(""));
                row.createCell(4).setCellValue(entry.getValue().stream().map(BulkResourceValidationResponse.BulkResourceRowError::description).reduce((a, b) -> a + " | " + b).orElse(""));
            }
            sheet.setColumnWidth(3, 7_000);
            sheet.setColumnWidth(4, 16_000);
            book.write(out);
            return out.toByteArray();
        } catch (Exception error) {
            throw new IllegalArgumentException("No fue posible anotar el archivo", error);
        }
    }

    private static boolean headersAreValid(Row row) {
        if (row == null) return false;
        for (int i = 0; i < HEADERS.size(); i++) if (!HEADERS.get(i).equals(cellValue(row.getCell(i)))) return false;
        return true;
    }

    private static CellStyle headerStyle(Workbook book) {
        CellStyle style = book.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = book.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private static String cell(Row row, int col, int number, List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
        if (row == null) return "";
        Cell c = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (c == null) return "";
        if (c.getCellType() == CellType.FORMULA || c.getCellType() == CellType.ERROR) {
            errors.add(error(number, HEADERS.get(col), "", "UNSUPPORTED_CELL", "No se admiten fórmulas ni celdas de error"));
            return "";
        }
        if (c.getCellType() != CellType.STRING) {
            errors.add(error(number, HEADERS.get(col), "", "UNEXPECTED_CELL_TYPE", "La celda debe contener texto"));
            return "";
        }
        return c.getStringCellValue();
    }

    private static String cellValue(Cell cell) {
        return cell == null || cell.getCellType() != CellType.STRING ? "" : cell.getStringCellValue();
    }

    private static BulkResourceValidationResponse.BulkResourceRowError error(int row, String field, String value, String code, String description) {
        return new BulkResourceValidationResponse.BulkResourceRowError(row, field, value.length() > 100 ? value.substring(0, 100) : value, code, description);
    }

    public record RowValue(int row, String applicationName, String path, String method) {
    }

    public record ParsedWorkbook(List<RowValue> rows,
                                 List<BulkResourceValidationResponse.BulkResourceRowError> errors) {
    }
}
