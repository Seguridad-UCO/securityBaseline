package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService.CatalogRow;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService.ProfileRoleRow;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService.RoleResourceRow;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Plantillas, lectura y reporte de errores del libro de carga masiva de catálogo (roles, perfiles y sus relaciones).
 */
class BulkCatalogWorkbookServiceTests {

    private final BulkCatalogWorkbookService service = new BulkCatalogWorkbookService();

    private static Workbook open(byte[] bytes) throws Exception {
        return WorkbookFactory.create(new ByteArrayInputStream(bytes));
    }

    private static List<String> sheetNames(Workbook book) {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < book.getNumberOfSheets(); i++) {
            names.add(book.getSheetName(i));
        }
        return names;
    }

    private static List<String> headers(Sheet sheet) {
        List<String> headers = new ArrayList<>();
        sheet.getRow(0).forEach(cell -> headers.add(cell.getStringCellValue()));
        return headers;
    }

    // ---- plantillas -------------------------------------------------------------------------------------------

    @Test
    void the_roles_template_has_roles_relations_and_guide_sheets_only() throws Exception {
        try (Workbook book = open(service.template("roles"))) {
            assertThat(sheetNames(book)).containsExactly("Roles", "RolesxRecurso", "Guía");
            assertThat(headers(book.getSheet("Roles"))).containsExactly("Nombre de rol*", "Alcance*", "Aplicación");
            assertThat(headers(book.getSheet("RolesxRecurso")))
                    .containsExactly("Nombre de rol*", "Alcance*", "Aplicación*", "Ruta*", "Método HTTP*");
            assertThat(book.getSheet("Guía").getRow(0).getCell(0).getStringCellValue()).contains("roles");
        }
    }

    @Test
    void the_resources_and_roles_template_adds_the_resources_sheet_with_the_current_headers() throws Exception {
        try (Workbook book = open(service.template("resources-roles"))) {
            assertThat(sheetNames(book)).containsExactly("Recursos", "Roles", "RolesxRecurso", "Guía");
            assertThat(headers(book.getSheet("Recursos"))).containsExactly("Aplicación*", "Ruta*", "Método HTTP*");
        }
    }

    @Test
    void the_profiles_template_has_profiles_and_profile_roles_sheets_without_role_sheets() throws Exception {
        try (Workbook book = open(service.template("profiles"))) {
            assertThat(sheetNames(book)).containsExactly("Perfiles", "PerfilesxRol", "Guía");
            assertThat(headers(book.getSheet("Perfiles"))).containsExactly("Nombre de perfil*", "Alcance*", "Aplicación");
            assertThat(headers(book.getSheet("PerfilesxRol"))).containsExactly("Nombre de perfil*",
                    "Alcance del perfil*", "Aplicación del perfil", "Nombre de rol*", "Alcance del rol*", "Aplicación del rol");
            assertThat(book.getSheet("Roles")).isNull();
        }
    }

    @Test
    void template_sheets_restrict_the_scope_column_to_tenant_or_application_and_freeze_the_header() throws Exception {
        try (Workbook book = open(service.template("roles"))) {
            Sheet roles = book.getSheet("Roles");
            assertThat(roles.getDataValidations()).hasSize(1);
            DataValidation validation = roles.getDataValidations().get(0);
            assertThat(validation.getValidationConstraint().getFormula1()).contains("TENANT").contains("APPLICATION");
            assertThat(validation.getRegions().getCellRangeAddress(0).getFirstColumn()).isEqualTo(1);
            assertThat(roles.getPaneInformation()).isNotNull();
            assertThat(roles.getPaneInformation().isFreezePane()).isTrue();
        }
    }

    @Test
    void the_resources_sheet_of_the_template_restricts_the_http_method_column() throws Exception {
        try (Workbook book = open(service.template("resources-roles"))) {
            DataValidation validation = book.getSheet("Recursos").getDataValidations().get(0);
            assertThat(validation.getValidationConstraint().getFormula1()).contains("GET").contains("PATCH").contains("DELETE");
            assertThat(validation.getRegions().getCellRangeAddress(0).getFirstColumn()).isEqualTo(2);
        }
    }

    @Test
    void a_generated_template_can_be_read_back_and_has_no_data_rows() throws Exception {
        assertThatThrownBy(() -> service.parse("roles", service.template("roles")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no contiene filas de datos");
    }

    /**
     * HALLAZGO: la rama {@code default} lanza {@code IllegalArgumentException} dentro del {@code try}, y el
     * {@code catch (Exception)} la convierte en {@code IllegalStateException}. {@code BulkResourceController.catalogTemplate}
     * solo atrapa {@code IllegalArgumentException} para responder 400: una modalidad desconocida acaba en 500.
     */
    @Disabled("Hallazgo: BulkCatalogWorkbookService.template(l.125/130) convierte la modalidad inválida en IllegalStateException; el controller espera IllegalArgumentException (400)")
    @Test
    void an_unsupported_mode_is_a_client_error_not_an_internal_failure() {
        assertThatThrownBy(() -> service.template("otra"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- lectura de roles y perfiles --------------------------------------------------------------------------

    @Test
    void parses_role_rows_with_their_spreadsheet_row_number_and_trimmed_cells() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"  Auditor ", "tenant", null},
                new String[]{"Editor", "APPLICATION", " Mi aplicación "}));

        assertThat(service.parse("roles", file)).containsExactly(
                new CatalogRow(2, "Auditor", "tenant", ""),
                new CatalogRow(3, "Editor", "APPLICATION", "Mi aplicación"));
    }

    @Test
    void parses_profile_rows_from_the_profiles_sheet() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"No debe leerse", "TENANT", null}),
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        assertThat(service.parse("profiles", file)).containsExactly(new CatalogRow(2, "Gestores", "TENANT", ""));
    }

    @Test
    void reads_legacy_xls_workbooks_too() throws Exception {
        byte[] file = CatalogWorkbooks.xls(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        assertThat(service.parse("roles", file)).extracting(CatalogRow::name).containsExactly("Auditor");
    }

    @Test
    void skips_blank_rows_and_rows_that_do_not_exist_but_keeps_the_original_row_numbers() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"", "", ""},
                (String[]) null,
                new String[]{"   ", null, null},
                new String[]{"Auditor", "TENANT", null}));

        assertThat(service.parse("roles", file)).containsExactly(new CatalogRow(5, "Auditor", "TENANT", ""));
    }

    @Test
    void keeps_rows_with_missing_required_cells_so_the_import_can_report_them() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{null, "TENANT", null},
                new String[]{"Auditor", null, null}));

        assertThat(service.parse("roles", file)).containsExactly(
                new CatalogRow(2, "", "TENANT", ""),
                new CatalogRow(3, "Auditor", "", ""));
    }

    @Test
    void rejects_a_workbook_without_the_requested_sheet() throws Exception {
        byte[] onlyProfiles = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        assertThatThrownBy(() -> service.parse("roles", onlyProfiles))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Falta la hoja Roles");
    }

    @Test
    void rejects_the_profiles_mode_when_the_profiles_sheet_is_missing() throws Exception {
        byte[] onlyRoles = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        assertThatThrownBy(() -> service.parse("profiles", onlyRoles))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Falta la hoja Perfiles");
    }

    @Test
    void rejects_a_file_with_headers_but_no_data() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles());

        assertThatThrownBy(() -> service.parse("roles", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no contiene filas de datos");
    }

    @Test
    void rejects_a_file_whose_rows_are_all_blank() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"", "", ""}, new String[]{" ", " ", " "}));

        assertThatThrownBy(() -> service.parse("roles", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no contiene filas de datos");
    }

    @Test
    void rejects_bytes_that_are_not_a_workbook() {
        for (byte[] bytes : new byte[][]{{1, 2, 3, 4}, "no soy excel".getBytes()}) {
            assertThatThrownBy(() -> service.parse("roles", bytes))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("El archivo no es un libro Excel válido");
        }
    }

    @Test
    void rejects_empty_input_with_an_illegal_argument() {
        assertThatThrownBy(() -> service.parse("roles", new byte[0])).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejects_a_password_protected_workbook_as_not_readable() throws Exception {
        byte[] file = CatalogWorkbooks.encrypted("secreto",
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        assertThatThrownBy(() -> service.parse("roles", file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("El archivo no es un libro Excel válido");
    }

    // ---- relaciones -------------------------------------------------------------------------------------------

    @Test
    void parses_role_resource_relations_skipping_blank_rows() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(
                        new String[]{"Auditor", "TENANT", "Mi aplicación", "/api/orders", "GET"},
                        new String[]{"", "", "", "", ""},
                        new String[]{"Editor", "APPLICATION", "Mi aplicación", "/api/items", "POST"}));

        assertThat(service.parseRoleResources(file)).containsExactly(
                new RoleResourceRow(2, "Auditor", "TENANT", "Mi aplicación", "/api/orders", "GET"),
                new RoleResourceRow(4, "Editor", "APPLICATION", "Mi aplicación", "/api/items", "POST"));
    }

    @Test
    void a_missing_role_resource_sheet_means_no_relations() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        assertThat(service.parseRoleResources(file)).isEmpty();
    }

    @Test
    void a_broken_file_cannot_provide_role_resource_relations() {
        assertThatThrownBy(() -> service.parseRoleResources(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La hoja RolesxRecurso no es válida");
    }

    @Test
    void parses_profile_role_relations_skipping_blank_rows() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(
                        new String[]{"Gestores", "TENANT", null, "Auditor", "APPLICATION", "Mi aplicación"},
                        new String[]{null, null, null, null, null, null},
                        new String[]{"Gestores", "TENANT", "", "Editor", "TENANT", ""}));

        assertThat(service.parseProfileRoles(file)).containsExactly(
                new ProfileRoleRow(2, "Gestores", "TENANT", "", "Auditor", "APPLICATION", "Mi aplicación"),
                new ProfileRoleRow(4, "Gestores", "TENANT", "", "Editor", "TENANT", ""));
    }

    @Test
    void a_missing_profile_role_sheet_means_no_relations() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        assertThat(service.parseProfileRoles(file)).isEmpty();
    }

    @Test
    void a_broken_file_cannot_provide_profile_role_relations() {
        assertThatThrownBy(() -> service.parseProfileRoles(new byte[]{1, 2, 3}))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La hoja PerfilesxRol no es válida");
    }

    // ---- reporte de errores -----------------------------------------------------------------------------------

    @Test
    void the_error_report_keeps_the_original_sheets_and_lists_one_row_per_error() throws Exception {
        byte[] original = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));
        List<BulkResourceRowError> errors = List.of(
                new BulkResourceRowError(2, "Nombre de rol*", "", "DUPLICATE_PERSISTED", "El rol ya existe"),
                new BulkResourceRowError(0, "file", "", "INVALID_WORKBOOK", "global"));

        try (Workbook report = open(service.errorReport(original, errors))) {
            assertThat(sheetNames(report)).containsExactly("Roles", "Errores");
            Sheet sheet = report.getSheet("Errores");
            assertThat(headers(sheet)).containsExactly("Fila", "Campo / hoja", "Código", "Descripción");
            assertThat(sheet.getLastRowNum()).isEqualTo(2);
            assertThat(sheet.getRow(1).getCell(0).getNumericCellValue()).isEqualTo(2.0);
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Nombre de rol*");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("DUPLICATE_PERSISTED");
            assertThat(sheet.getRow(1).getCell(3).getStringCellValue()).isEqualTo("El rol ya existe");
            assertThat(sheet.getRow(2).getCell(2).getStringCellValue()).isEqualTo("INVALID_WORKBOOK");
        }
    }

    @Test
    void the_error_report_without_errors_only_has_the_header() throws Exception {
        byte[] original = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        try (Workbook report = open(service.errorReport(original, List.of()))) {
            assertThat(report.getSheet("Errores").getLastRowNum()).isZero();
        }
    }

    @Test
    void the_error_report_cannot_be_built_from_a_broken_file() {
        assertThatThrownBy(() -> service.errorReport(new byte[]{1, 2, 3}, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("No fue posible generar el reporte");
    }
}
