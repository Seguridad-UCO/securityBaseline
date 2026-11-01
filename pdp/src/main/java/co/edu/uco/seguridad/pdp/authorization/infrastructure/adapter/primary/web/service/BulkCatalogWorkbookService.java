package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantillas independientes del catálogo: nunca añade hojas irrelevantes a una modalidad.
 *
 * <p>La lectura no lanza excepciones: cada problema del archivo, de una hoja o de una celda vuelve como un
 * error con código estable dentro de {@link Parsed}. Las cabeceras de cada hoja son una única fuente, usada
 * tanto por la plantilla como por la lectura.</p>
 */
public final class BulkCatalogWorkbookService {
    public static final int MAX_BYTES = BulkResourceWorkbookService.MAX_BYTES;
    public static final int MAX_ROWS = BulkResourceWorkbookService.MAX_ROWS;

    private static final String ROLES_SHEET = "Roles";
    private static final String PROFILES_SHEET = "Perfiles";
    private static final String ROLE_RESOURCES_SHEET = "RolesxRecurso";
    private static final String PROFILE_ROLES_SHEET = "PerfilesxRol";
    private static final String ERRORS_SHEET = "Errores";

    private static final List<String> RESOURCES_HEADERS = List.of("Aplicación*", "Ruta*", "Método HTTP*");
    private static final List<String> ROLES_HEADERS = List.of("Nombre de rol*", "Alcance*", "Aplicación");
    private static final List<String> PROFILES_HEADERS = List.of("Nombre de perfil*", "Alcance*", "Aplicación");
    private static final List<String> ROLE_RESOURCES_HEADERS = List.of("Nombre de rol*", "Alcance*", "Aplicación*", "Ruta*", "Método HTTP*");
    private static final List<String> PROFILE_ROLES_HEADERS = List.of("Nombre de perfil*", "Alcance del perfil*", "Aplicación del perfil", "Nombre de rol*", "Alcance del rol*", "Aplicación del rol");

    private static final String FILE_FIELD = "file";
    private static final String EMPTY_FILE_MESSAGE = "El archivo está vacío";
    private static final String NO_DATA_ROWS_MESSAGE = "El archivo no contiene filas de datos";
    private static final String FILE_TOO_LARGE_MESSAGE = "El archivo supera el límite de 5 MB";
    private static final String ENCRYPTED_FILE_MESSAGE = "No se aceptan libros cifrados";
    private static final String INVALID_WORKBOOK_MESSAGE = "El archivo no es un libro Excel válido";
    private static final String UNEXPECTED_CELL_TYPE_MESSAGE = "La celda debe contener texto";
    private static final String UNSUPPORTED_CELL_MESSAGE = "No se admiten fórmulas ni celdas de error";
    private static final String UNSUPPORTED_ROLES_OR_PROFILES_MODE_MESSAGE = "Modalidad de carga no soportada: use roles o profiles";
    private static final String UNSUPPORTED_TEMPLATE_MODE_MESSAGE = "Modalidad de carga no soportada: use roles, profiles o resources-roles";

    /** Resultado de leer una hoja: las filas conservadas y los problemas encontrados al leerlas. */
    public record Parsed<T>(List<T> rows, List<BulkResourceRowError> errors) {
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        T map(int row, String[] cells);
    }

    public Parsed<CatalogRow> parse(String mode, byte[] bytes) {
        if ("roles".equals(mode)) {
            return read(bytes, ROLES_SHEET, ROLES_HEADERS, true, (row, c) -> new CatalogRow(row, c[0], c[1], c[2]));
        }
        if ("profiles".equals(mode)) {
            return read(bytes, PROFILES_SHEET, PROFILES_HEADERS, true, (row, c) -> new CatalogRow(row, c[0], c[1], c[2]));
        }
        throw new UnsupportedBulkModeException(UNSUPPORTED_ROLES_OR_PROFILES_MODE_MESSAGE);
    }

    public Parsed<RoleResourceRow> parseRoleResources(byte[] bytes) {
        return read(bytes, ROLE_RESOURCES_SHEET, ROLE_RESOURCES_HEADERS, false,
                (row, c) -> new RoleResourceRow(row, c[0], c[1], c[2], c[3], c[4]));
    }

    public Parsed<ProfileRoleRow> parseProfileRoles(byte[] bytes) {
        return read(bytes, PROFILE_ROLES_SHEET, PROFILE_ROLES_HEADERS, false,
                (row, c) -> new ProfileRoleRow(row, c[0], c[1], c[2], c[3], c[4], c[5]));
    }

    public record CatalogRow(int row, String name, String scope, String applicationId) {
    }

    public record RoleResourceRow(int row, String role, String scope, String application, String path, String method) {
    }

    public record ProfileRoleRow(int row, String profile, String profileScope, String profileApplication, String role,
                                 String roleScope, String roleApplication) {
    }

    /**
     * Lee una hoja. La hoja principal (roles o perfiles) es obligatoria y no puede estar vacía; las de relación
     * son opcionales. Una celda ilegible se reporta y su fila se conserva con la celda vacía.
     */
    private <T> Parsed<T> read(byte[] bytes, String sheetName, List<String> headers, boolean main, RowMapper<T> mapper) {
        if (bytes == null || bytes.length == 0) return failure("EMPTY_FILE", EMPTY_FILE_MESSAGE);
        if (bytes.length > MAX_BYTES) return failure("FILE_TOO_LARGE", FILE_TOO_LARGE_MESSAGE);
        try (Workbook book = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = book.getSheet(sheetName);
            if (sheet == null) {
                return main ? failure("MISSING_SHEET", "Falta la hoja " + sheetName) : new Parsed<>(List.of(), List.of());
            }
            if (!headersAreValid(sheet.getRow(0), headers)) {
                return new Parsed<>(List.of(), List.of(error(1, sheetName, "INVALID_HEADERS",
                        "Los encabezados de la hoja " + sheetName + " deben ser: " + String.join(", ", headers))));
            }
            List<T> rows = new ArrayList<>();
            List<BulkResourceRowError> errors = new ArrayList<>();
            int last = sheet.getLastRowNum();
            if (last > MAX_ROWS) {
                errors.add(error(0, sheetName, "TOO_MANY_ROWS", "La hoja " + sheetName + " supera las 1.000 filas de datos"));
            }
            for (int index = 1; index <= Math.min(last, MAX_ROWS); index++) {
                Row row = sheet.getRow(index);
                if (row == null) continue;
                String[] cells = new String[headers.size()];
                boolean hasContent = false;
                boolean unreadable = false;
                for (int column = 0; column < cells.length; column++) {
                    String field = main ? headers.get(column) : sheetName;
                    Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    cells[column] = "";
                    if (cell == null) continue;
                    CellType type = cell.getCellType();
                    if (type == CellType.STRING) {
                        cells[column] = cell.getStringCellValue().trim();
                        hasContent |= !cells[column].isEmpty();
                    } else if (type == CellType.FORMULA || type == CellType.ERROR) {
                        errors.add(error(index + 1, field, "UNSUPPORTED_CELL", UNSUPPORTED_CELL_MESSAGE));
                        unreadable = true;
                    } else if (type != CellType.BLANK) {
                        errors.add(error(index + 1, field, "UNEXPECTED_CELL_TYPE", UNEXPECTED_CELL_TYPE_MESSAGE));
                        unreadable = true;
                    }
                }
                if (hasContent || unreadable) rows.add(mapper.map(index + 1, cells));
            }
            if (main && rows.isEmpty()) errors.add(error(0, FILE_FIELD, "EMPTY_FILE", NO_DATA_ROWS_MESSAGE));
            return new Parsed<>(rows, errors);
        } catch (EncryptedDocumentException encrypted) {
            return failure("ENCRYPTED_FILE", ENCRYPTED_FILE_MESSAGE);
        } catch (Exception unreadable) {
            return failure("INVALID_WORKBOOK", INVALID_WORKBOOK_MESSAGE);
        }
    }

    private static <T> Parsed<T> failure(String code, String description) {
        return new Parsed<>(List.of(), List.of(error(0, FILE_FIELD, code, description)));
    }

    private static boolean headersAreValid(Row row, List<String> headers) {
        if (row == null) return false;
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.getCell(i);
            String value = cell == null || cell.getCellType() != CellType.STRING ? "" : cell.getStringCellValue();
            if (!headers.get(i).equals(value)) return false;
        }
        return true;
    }

    private static BulkResourceRowError error(int row, String field, String code, String description) {
        return new BulkResourceRowError(row, field, "", code, description);
    }

    /** Si el original no se puede abrir (vacío, corrupto, cifrado), el reporte es un libro solo con la hoja de errores. */
    public byte[] errorReport(byte[] bytes, List<BulkResourceRowError> errors) {
        try (Workbook book = openOrEmpty(bytes); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet(ERRORS_SHEET);
            Row head = sheet.createRow(0);
            String[] columns = {"Fila", "Campo / hoja", "Código", "Descripción"};
            for (int i = 0; i < columns.length; i++) head.createCell(i).setCellValue(columns[i]);
            int row = 1;
            for (var error : errors) {
                Row target = sheet.createRow(row++);
                target.createCell(0).setCellValue(error.row());
                target.createCell(1).setCellValue(error.field());
                target.createCell(2).setCellValue(error.code());
                target.createCell(3).setCellValue(error.description());
            }
            for (int i = 0; i < 4; i++) sheet.setColumnWidth(i, 9_000);
            book.write(output);
            return output.toByteArray();
        } catch (Exception e) {
            throw new IllegalArgumentException("No fue posible generar el reporte", e);
        }
    }

    private static Workbook openOrEmpty(byte[] bytes) {
        if (bytes != null && bytes.length > 0 && bytes.length <= MAX_BYTES) {
            try {
                return WorkbookFactory.create(new ByteArrayInputStream(bytes));
            } catch (Exception unreadable) {
                // El reporte sigue siendo útil sin el original: se entrega solo con la hoja de errores.
            }
        }
        return new XSSFWorkbook();
    }

    public byte[] template(String mode) {
        if (!"roles".equals(mode) && !"profiles".equals(mode) && !"resources-roles".equals(mode)) {
            throw new UnsupportedBulkModeException(UNSUPPORTED_TEMPLATE_MODE_MESSAGE);
        }
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            switch (mode) {
                case "resources-roles" -> {
                    resources(book);
                    roles(book);
                    relations(book, ROLE_RESOURCES_SHEET);
                }
                case "roles" -> {
                    roles(book);
                    relations(book, ROLE_RESOURCES_SHEET);
                }
                default -> {
                    profiles(book);
                    profileRoles(book);
                }
            }
            guide(book, mode);
            book.write(output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible generar la plantilla", error);
        }
    }

    private static void resources(Workbook book) {
        sheet(book, "Recursos", RESOURCES_HEADERS, 2, List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
    }

    private static void roles(Workbook book) {
        sheet(book, ROLES_SHEET, ROLES_HEADERS, 1, List.of("TENANT", "APPLICATION"));
    }

    private static void relations(Workbook book, String name) {
        sheet(book, name, ROLE_RESOURCES_HEADERS, 1, List.of("TENANT", "APPLICATION"));
    }

    private static void profiles(Workbook book) {
        sheet(book, PROFILES_SHEET, PROFILES_HEADERS, 1, List.of("TENANT", "APPLICATION"));
    }

    private static void profileRoles(Workbook book) {
        sheet(book, PROFILE_ROLES_SHEET, PROFILE_ROLES_HEADERS, 1, List.of("TENANT", "APPLICATION"));
    }

    private static void sheet(Workbook book, String name, List<String> headers, int validationColumn, List<String> options) {
        Sheet sheet = book.createSheet(name);
        CellStyle style = book.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = book.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(style);
            sheet.setColumnWidth(i, 8_000);
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, 1000, 0, headers.size() - 1));
        DataValidation validation = sheet.getDataValidationHelper().createValidation(sheet.getDataValidationHelper().createExplicitListConstraint(options.toArray(String[]::new)), new CellRangeAddressList(1, 1000, validationColumn, validationColumn));
        sheet.addValidationData(validation);
    }

    private static void guide(Workbook book, String mode) {
        Sheet sheet = book.createSheet("Guía");
        sheet.setColumnWidth(0, 30_000);
        sheet.createRow(0).createCell(0).setCellValue("Carga masiva: " + mode);
        sheet.createRow(1).createCell(0).setCellValue("No modifique encabezados. Los campos con * son obligatorios.");
        sheet.createRow(2).createCell(0).setCellValue("APPLICATION requiere aplicación; TENANT no la requiere.");
        sheet.createRow(3).createCell(0).setCellValue("Límites: el archivo puede pesar hasta 5 MB y tener hasta 1.000 filas de datos por hoja.");
    }
}
