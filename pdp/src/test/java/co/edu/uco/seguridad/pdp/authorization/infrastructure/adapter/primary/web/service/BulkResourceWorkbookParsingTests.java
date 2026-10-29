package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lectura y anotación del libro de carga masiva: el contrato de columnas, duplicados y errores por fila. */
class BulkResourceWorkbookParsingTests {

    private static final String[] HEADERS = {"Nombre de aplicación", "Ruta", "Método HTTP"};

    private final BulkResourceWorkbookService service = new BulkResourceWorkbookService();

    private static byte[] book(String sheetName, String[] headers, String[]... rows) throws Exception {
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet(sheetName);
            Row header = sheet.createRow(0);
            for (int c = 0; c < headers.length; c++) {
                header.createCell(c).setCellValue(headers[c]);
            }
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            book.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void parses_data_rows_with_their_spreadsheet_row_number() throws Exception {
        var parsed = service.parse(book("Recursos", HEADERS,
                new String[]{"App", "/a", "GET"}, new String[]{"App", "/b", "POST"}));

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).extracting(r -> r.row()).containsExactly(2, 3);
        assertThat(parsed.rows().get(1).path()).isEqualTo("/b");
    }

    @Test
    void skips_blank_rows_and_flags_missing_required_cells() throws Exception {
        var parsed = service.parse(book("Recursos", HEADERS,
                new String[]{"", "", ""}, new String[]{"App", "", "GET"}));

        assertThat(parsed.errors()).extracting(BulkResourceRowError::code).containsExactly("REQUIRED");
        assertThat(parsed.errors().get(0).row()).isEqualTo(3);
    }

    @Test
    void flags_a_duplicate_inside_the_file_ignoring_case_of_name_and_verb() throws Exception {
        var parsed = service.parse(book("Recursos", HEADERS,
                new String[]{"App", "/a", "get"}, new String[]{"APP", "/a", "GET"}));

        assertThat(parsed.errors()).extracting(BulkResourceRowError::code).containsExactly("DUPLICATE_IN_FILE");
    }

    @Test
    void rejects_a_workbook_without_the_resources_sheet() throws Exception {
        var parsed = service.parse(book("Otra", HEADERS, new String[]{"App", "/a", "GET"}));

        assertThat(parsed.errors()).extracting(BulkResourceRowError::code).containsExactly("MISSING_SHEET");
    }

    @Test
    void rejects_changed_headers() throws Exception {
        var parsed = service.parse(book("Recursos", new String[]{"App", "Ruta", "Método HTTP"},
                new String[]{"App", "/a", "GET"}));

        assertThat(parsed.errors()).extracting(BulkResourceRowError::code).containsExactly("INVALID_HEADERS");
    }

    @Test
    void rejects_a_file_with_headers_but_no_data() throws Exception {
        var parsed = service.parse(book("Recursos", HEADERS));

        assertThat(parsed.errors()).extracting(BulkResourceRowError::code).containsExactly("EMPTY_FILE");
    }

    @Test
    void rejects_a_file_above_the_size_limit() {
        var parsed = service.parse(new byte[BulkResourceWorkbookService.MAX_BYTES + 1]);

        assertThat(parsed.errors()).extracting(BulkResourceRowError::code).containsExactly("FILE_TOO_LARGE");
    }

    @Test
    void rejects_bytes_that_are_not_a_workbook() {
        var parsed = service.parse(new byte[]{1, 2, 3, 4});

        assertThat(parsed.errors()).extracting(BulkResourceRowError::code).containsExactly("INVALID_WORKBOOK");
    }

    @Test
    void reports_formula_cells_as_unsupported() throws Exception {
        byte[] file;
        try (Workbook book = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = book.createSheet("Recursos");
            Row header = sheet.createRow(0);
            for (int c = 0; c < HEADERS.length; c++) {
                header.createCell(c).setCellValue(HEADERS[c]);
            }
            Row row = sheet.createRow(1);
            row.createCell(0).setCellFormula("\"A\"&\"pp\"");
            row.createCell(1).setCellValue("/a");
            row.createCell(2).setCellValue("GET");
            book.write(out);
            file = out.toByteArray();
        }

        assertThat(service.parse(file).errors()).extracting(BulkResourceRowError::code).contains("UNSUPPORTED_CELL");
    }

    @Test
    void annotates_each_failing_row_with_its_codes_and_descriptions() throws Exception {
        byte[] original = book("Recursos", HEADERS, new String[]{"App", "/a", "GET"});
        List<BulkResourceRowError> errors = List.of(
                new BulkResourceRowError(2, "Ruta", "/a", "REQUIRED", "La ruta es obligatoria"),
                new BulkResourceRowError(2, "row", "", "DUPLICATE_PERSISTED", "El recurso ya existe"),
                new BulkResourceRowError(0, "file", "", "EMPTY_FILE", "global"));

        try (Workbook annotated = WorkbookFactory.create(new ByteArrayInputStream(service.annotate(original, errors)))) {
            Sheet sheet = annotated.getSheet("Recursos");
            assertThat(sheet.getRow(0).getCell(3).getStringCellValue()).isEqualTo("Código de error");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("REQUIRED, DUPLICATE_PERSISTED");
            assertThat(sheet.getRow(1).getCell(4).getStringCellValue())
                    .contains("La ruta es obligatoria").contains("El recurso ya existe");
        }
    }

    @Test
    void annotating_a_workbook_without_the_sheet_fails_clearly() throws Exception {
        byte[] other = book("Otra", HEADERS);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.annotate(other, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
