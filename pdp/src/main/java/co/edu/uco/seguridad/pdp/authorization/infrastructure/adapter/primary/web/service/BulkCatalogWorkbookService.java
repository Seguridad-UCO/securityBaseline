package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Plantillas independientes del catálogo: nunca añade hojas irrelevantes a una modalidad.
 */
public final class BulkCatalogWorkbookService {
    public List<CatalogRow> parse(String mode, byte[] bytes) {
        String sheetName = mode.equals("profiles") ? "Perfiles" : "Roles";
        try (Workbook book = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = book.getSheet(sheetName);
            if (sheet == null) throw new IllegalArgumentException("Falta la hoja " + sheetName);
            List<CatalogRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String name = value(row, 0), scope = value(row, 1), app = value(row, 2);
                if (name.isBlank() && scope.isBlank() && app.isBlank()) continue;
                rows.add(new CatalogRow(i + 1, name, scope, app));
            }
            if (rows.isEmpty()) throw new IllegalArgumentException("El archivo no contiene filas de datos");
            return rows;
        } catch (IllegalArgumentException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalArgumentException("El archivo no es un libro Excel válido", error);
        }
    }

    private static String value(Row row, int index) {
        if (row == null || row.getCell(index) == null) return "";
        Cell cell = row.getCell(index);
        return cell.getCellType() == CellType.STRING ? cell.getStringCellValue().trim() : "";
    }

    public record CatalogRow(int row, String name, String scope, String applicationId) {
    }

    public List<RoleResourceRow> parseRoleResources(byte[] bytes) {
        try (Workbook book = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = book.getSheet("RolesxRecurso");
            if (sheet == null) return List.of();
            List<RoleResourceRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String role = value(row, 0), scope = value(row, 1), app = value(row, 2), path = value(row, 3), method = value(row, 4);
                if (role.isBlank() && scope.isBlank() && app.isBlank() && path.isBlank() && method.isBlank()) continue;
                rows.add(new RoleResourceRow(i + 1, role, scope, app, path, method));
            }
            return rows;
        } catch (Exception error) {
            throw new IllegalArgumentException("La hoja RolesxRecurso no es válida", error);
        }
    }

    public record RoleResourceRow(int row, String role, String scope, String application, String path, String method) {
    }

    public List<ProfileRoleRow> parseProfileRoles(byte[] bytes) {
        try (Workbook book = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            Sheet sheet = book.getSheet("PerfilesxRol");
            if (sheet == null) return List.of();
            List<ProfileRoleRow> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                String pn = value(row, 0), ps = value(row, 1), pa = value(row, 2), rn = value(row, 3), rs = value(row, 4), ra = value(row, 5);
                if (pn.isBlank() && ps.isBlank() && pa.isBlank() && rn.isBlank() && rs.isBlank() && ra.isBlank())
                    continue;
                rows.add(new ProfileRoleRow(i + 1, pn, ps, pa, rn, rs, ra));
            }
            return rows;
        } catch (Exception e) {
            throw new IllegalArgumentException("La hoja PerfilesxRol no es válida", e);
        }
    }

    public record ProfileRoleRow(int row, String profile, String profileScope, String profileApplication, String role,
                                 String roleScope, String roleApplication) {
    }

    public byte[] errorReport(byte[] bytes, List<co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError> errors) {
        try (Workbook book = WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes)); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Errores");
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

    public byte[] template(String mode) {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            switch (mode) {
                case "resources-roles" -> {
                    resources(book);
                    roles(book);
                    relations(book, "RolesxRecurso");
                }
                case "roles" -> {
                    roles(book);
                    relations(book, "RolesxRecurso");
                }
                case "profiles" -> {
                    profiles(book);
                    profileRoles(book);
                }
                default -> throw new IllegalArgumentException("Modalidad de carga no soportada");
            }
            guide(book, mode);
            book.write(output);
            return output.toByteArray();
        } catch (Exception error) {
            throw new IllegalStateException("No fue posible generar la plantilla", error);
        }
    }

    private static void resources(Workbook book) {
        sheet(book, "Recursos", List.of("Aplicación*", "Ruta*", "Método HTTP*"), 2, List.of("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"));
    }

    private static void roles(Workbook book) {
        sheet(book, "Roles", List.of("Nombre de rol*", "Alcance*", "Aplicación"), 1, List.of("TENANT", "APPLICATION"));
    }

    private static void relations(Workbook book, String name) {
        sheet(book, name, List.of("Nombre de rol*", "Alcance*", "Aplicación*", "Ruta*", "Método HTTP*"), 1, List.of("TENANT", "APPLICATION"));
    }

    private static void profiles(Workbook book) {
        sheet(book, "Perfiles", List.of("Nombre de perfil*", "Alcance*", "Aplicación"), 1, List.of("TENANT", "APPLICATION"));
    }

    private static void profileRoles(Workbook book) {
        sheet(book, "PerfilesxRol", List.of("Nombre de perfil*", "Alcance del perfil*", "Aplicación del perfil", "Nombre de rol*", "Alcance del rol*", "Aplicación del rol"), 1, List.of("TENANT", "APPLICATION"));
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
    }
}
