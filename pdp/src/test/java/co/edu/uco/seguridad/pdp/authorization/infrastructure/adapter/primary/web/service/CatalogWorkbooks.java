package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * Libros de prueba de la carga masiva de catálogo. Cada hoja se describe con su nombre y sus filas;
 * una fila {@code null} deja un hueco (la fila no existe en el libro) y una celda {@code null} no se crea.
 */
final class CatalogWorkbooks {

    static final String[] ROLE_HEADERS = {"Nombre de rol*", "Alcance*", "Aplicación"};
    static final String[] PROFILE_HEADERS = {"Nombre de perfil*", "Alcance*", "Aplicación"};
    static final String[] ROLE_RESOURCE_HEADERS = {"Nombre de rol*", "Alcance*", "Aplicación*", "Ruta*", "Método HTTP*"};
    static final String[] PROFILE_ROLE_HEADERS = {"Nombre de perfil*", "Alcance del perfil*", "Aplicación del perfil",
            "Nombre de rol*", "Alcance del rol*", "Aplicación del rol"};

    private CatalogWorkbooks() {
    }

    record SheetData(String name, String[][] rows) {
    }

    static SheetData sheet(String name, String[] headers, String[]... rows) {
        String[][] all = new String[rows.length + 1][];
        all[0] = headers;
        System.arraycopy(rows, 0, all, 1, rows.length);
        return new SheetData(name, all);
    }

    static SheetData roles(String[]... rows) {
        return sheet("Roles", ROLE_HEADERS, rows);
    }

    static SheetData profiles(String[]... rows) {
        return sheet("Perfiles", PROFILE_HEADERS, rows);
    }

    static SheetData roleResources(String[]... rows) {
        return sheet("RolesxRecurso", ROLE_RESOURCE_HEADERS, rows);
    }

    static SheetData profileRoles(String[]... rows) {
        return sheet("PerfilesxRol", PROFILE_ROLE_HEADERS, rows);
    }

    static byte[] xlsx(SheetData... sheets) throws Exception {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fill(book, sheets);
            book.write(out);
            return out.toByteArray();
        }
    }

    static byte[] xls(SheetData... sheets) throws Exception {
        try (Workbook book = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            fill(book, sheets);
            book.write(out);
            return out.toByteArray();
        }
    }

    /** Libro xlsx cifrado con contraseña (cifrado Agile de Office): no se puede leer sin ella. */
    static byte[] encrypted(String password, SheetData... sheets) throws Exception {
        byte[] plain = xlsx(sheets);
        try (POIFSFileSystem fileSystem = new POIFSFileSystem(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Encryptor encryptor = new EncryptionInfo(EncryptionMode.agile).getEncryptor();
            encryptor.confirmPassword(password);
            try (OPCPackage pack = OPCPackage.open(new ByteArrayInputStream(plain));
                 OutputStream encrypted = encryptor.getDataStream(fileSystem)) {
                pack.save(encrypted);
            }
            fileSystem.writeFilesystem(out);
            return out.toByteArray();
        }
    }

    private static void fill(Workbook book, SheetData... sheets) {
        for (SheetData data : sheets) {
            Sheet sheet = book.createSheet(data.name());
            for (int r = 0; r < data.rows().length; r++) {
                String[] cells = data.rows()[r];
                if (cells == null) {
                    continue;
                }
                Row row = sheet.createRow(r);
                for (int c = 0; c < cells.length; c++) {
                    if (cells[c] != null) {
                        row.createCell(c).setCellValue(cells[c]);
                    }
                }
            }
        }
    }
}
