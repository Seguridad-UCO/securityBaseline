package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response.BulkResourceValidationResponse.BulkResourceRowError;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService.CatalogRow;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService.Parsed;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService.ProfileRoleRow;
import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service.BulkCatalogWorkbookService.RoleResourceRow;
import org.apache.poi.ss.usermodel.DataValidation;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Plantillas, lectura y reporte de errores del libro de carga masiva de catálogo (roles, perfiles y sus relaciones).
 * La lectura no lanza excepciones: cada problema del archivo o de una celda vuelve como un error con código estable.
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

    /** Fila, campo, código y descripción de un error: lo que el contrato fija (el {@code value} no se afirma). */
    private static void assertSingleError(List<BulkResourceRowError> errors, int row, String field, String code,
                                          String description) {
        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(row);
            assertThat(error.field()).isEqualTo(field);
            assertThat(error.code()).isEqualTo(code);
            assertThat(error.description()).isEqualTo(description);
        });
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
        Parsed<CatalogRow> parsed = service.parse("roles", service.template("roles"));

        assertThat(parsed.rows()).isEmpty();
        assertSingleError(parsed.errors(), 0, "file", "EMPTY_FILE", "El archivo no contiene filas de datos");
    }

    @Test
    void the_headers_of_every_template_sheet_are_the_ones_the_reader_demands() throws Exception {
        try (Workbook roles = open(service.template("roles")); Workbook profiles = open(service.template("profiles"))) {
            assertThat(headers(roles.getSheet("Roles"))).containsExactly(CatalogWorkbooks.ROLE_HEADERS);
            assertThat(headers(roles.getSheet("RolesxRecurso"))).containsExactly(CatalogWorkbooks.ROLE_RESOURCE_HEADERS);
            assertThat(headers(profiles.getSheet("Perfiles"))).containsExactly(CatalogWorkbooks.PROFILE_HEADERS);
            assertThat(headers(profiles.getSheet("PerfilesxRol"))).containsExactly(CatalogWorkbooks.PROFILE_ROLE_HEADERS);
        }
        // Ida y vuelta: la lectura no rechaza las cabeceras de la plantilla (solo reporta que no hay datos)
        assertThat(service.parse("profiles", service.template("profiles")).errors())
                .extracting(BulkResourceRowError::code).containsExactly("EMPTY_FILE");
        assertThat(service.parseRoleResources(service.template("roles"))).isEqualTo(new Parsed<RoleResourceRow>(List.of(), List.of()));
        assertThat(service.parseProfileRoles(service.template("profiles"))).isEqualTo(new Parsed<ProfileRoleRow>(List.of(), List.of()));
    }

    @Test
    void every_template_states_the_size_and_row_limits_in_its_guide_sheet() throws Exception {
        for (String mode : List.of("roles", "profiles", "resources-roles")) {
            try (Workbook book = open(service.template(mode))) {
                Sheet guide = book.getSheet("Guía");
                StringBuilder text = new StringBuilder();
                guide.forEach(row -> row.forEach(cell -> text.append(cell.getStringCellValue()).append('\n')));
                assertThat(text.toString()).contains("5 MB").contains("1.000");
            }
        }
    }

    @Test
    void an_unsupported_mode_is_a_client_error_not_an_internal_failure() {
        assertThatThrownBy(() -> service.template("otra"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void an_unsupported_template_mode_is_reported_as_an_unsupported_bulk_mode_whatever_its_spelling() {
        for (String mode : List.of("otra", "Roles", "PROFILES", "")) {
            assertThatThrownBy(() -> service.template(mode)).isInstanceOf(UnsupportedBulkModeException.class);
        }
    }

    @Test
    void the_limits_are_the_same_ones_the_resources_upload_uses() {
        assertThat(BulkCatalogWorkbookService.MAX_BYTES).isEqualTo(BulkResourceWorkbookService.MAX_BYTES).isEqualTo(5 * 1024 * 1024);
        assertThat(BulkCatalogWorkbookService.MAX_ROWS).isEqualTo(BulkResourceWorkbookService.MAX_ROWS).isEqualTo(1_000);
    }

    // ---- lectura de roles y perfiles --------------------------------------------------------------------------

    @Test
    void parses_role_rows_with_their_spreadsheet_row_number_and_trimmed_cells() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"  Auditor ", "tenant", null},
                new String[]{"Editor", "APPLICATION", " Mi aplicación "}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).containsExactly(
                new CatalogRow(2, "Auditor", "tenant", ""),
                new CatalogRow(3, "Editor", "APPLICATION", "Mi aplicación"));
    }

    @Test
    void parses_profile_rows_from_the_profiles_sheet() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"No debe leerse", "TENANT", null}),
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        Parsed<CatalogRow> parsed = service.parse("profiles", file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).containsExactly(new CatalogRow(2, "Gestores", "TENANT", ""));
    }

    @Test
    void reads_legacy_xls_workbooks_too() throws Exception {
        byte[] file = CatalogWorkbooks.xls(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).extracting(CatalogRow::name).containsExactly("Auditor");
    }

    @Test
    void skips_blank_rows_and_rows_that_do_not_exist_but_keeps_the_original_row_numbers() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{"", "", ""},
                (String[]) null,
                new String[]{"   ", null, null},
                new String[]{"Auditor", "TENANT", null}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).containsExactly(new CatalogRow(5, "Auditor", "TENANT", ""));
    }

    @Test
    void keeps_rows_with_missing_required_cells_so_the_import_can_report_them() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new String[]{null, "TENANT", null},
                new String[]{"Auditor", null, null}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).containsExactly(
                new CatalogRow(2, "", "TENANT", ""),
                new CatalogRow(3, "Auditor", "", ""));
    }

    @Test
    void extra_columns_after_the_expected_headers_are_allowed() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.sheet("Roles",
                new String[]{"Nombre de rol*", "Alcance*", "Aplicación", "Comentarios"},
                new String[]{"Auditor", "TENANT", null, "libre"}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).containsExactly(new CatalogRow(2, "Auditor", "TENANT", ""));
    }

    @Test
    void only_the_roles_and_profiles_modes_can_be_read() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        for (String mode : List.of("otra", "resources-roles", "Roles")) {
            assertThatThrownBy(() -> service.parse(mode, file)).isInstanceOf(UnsupportedBulkModeException.class);
        }
    }

    // ---- lectura: problemas del archivo -----------------------------------------------------------------------

    @Test
    void a_workbook_without_the_requested_sheet_reports_the_missing_sheet() throws Exception {
        byte[] onlyProfiles = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        Parsed<CatalogRow> parsed = service.parse("roles", onlyProfiles);

        assertThat(parsed.rows()).isEmpty();
        assertSingleError(parsed.errors(), 0, "file", "MISSING_SHEET", "Falta la hoja Roles");
    }

    @Test
    void the_profiles_mode_reports_the_missing_profiles_sheet() throws Exception {
        byte[] onlyRoles = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        Parsed<CatalogRow> parsed = service.parse("profiles", onlyRoles);

        assertThat(parsed.rows()).isEmpty();
        assertSingleError(parsed.errors(), 0, "file", "MISSING_SHEET", "Falta la hoja Perfiles");
    }

    @Test
    void a_file_with_headers_but_no_data_is_empty() throws Exception {
        Parsed<CatalogRow> parsed = service.parse("roles", CatalogWorkbooks.xlsx(CatalogWorkbooks.roles()));

        assertThat(parsed.rows()).isEmpty();
        assertSingleError(parsed.errors(), 0, "file", "EMPTY_FILE", "El archivo no contiene filas de datos");
    }

    @Test
    void a_file_whose_rows_are_all_blank_is_empty() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"", "", ""}, new String[]{" ", " ", " "}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.rows()).isEmpty();
        assertSingleError(parsed.errors(), 0, "file", "EMPTY_FILE", "El archivo no contiene filas de datos");
    }

    @Test
    void bytes_that_are_not_a_workbook_are_reported_instead_of_thrown() {
        for (byte[] bytes : new byte[][]{{1, 2, 3, 4}, "no soy excel".getBytes()}) {
            Parsed<CatalogRow> parsed = service.parse("roles", bytes);

            assertThat(parsed.rows()).isEmpty();
            assertSingleError(parsed.errors(), 0, "file", "INVALID_WORKBOOK", "El archivo no es un libro Excel válido");
        }
    }

    @Test
    void zero_bytes_and_null_are_an_empty_file_with_a_spanish_message() {
        for (byte[] bytes : new byte[][]{new byte[0], null}) {
            Parsed<CatalogRow> parsed = service.parse("roles", bytes);

            assertThat(parsed.rows()).isEmpty();
            assertSingleError(parsed.errors(), 0, "file", "EMPTY_FILE", "El archivo está vacío");
        }
    }

    @Test
    void a_file_over_the_size_limit_is_too_large_and_the_limit_itself_is_still_inspected() {
        Parsed<CatalogRow> tooLarge = service.parse("roles", new byte[BulkCatalogWorkbookService.MAX_BYTES + 1]);
        Parsed<CatalogRow> atTheLimit = service.parse("roles", new byte[BulkCatalogWorkbookService.MAX_BYTES]);

        assertThat(tooLarge.rows()).isEmpty();
        assertSingleError(tooLarge.errors(), 0, "file", "FILE_TOO_LARGE", "El archivo supera el límite de 5 MB");
        assertThat(atTheLimit.errors()).singleElement()
                .satisfies(error -> assertThat(error.code()).isEqualTo("INVALID_WORKBOOK"));
    }

    @Test
    void a_password_protected_workbook_is_reported_as_encrypted() throws Exception {
        byte[] file = CatalogWorkbooks.encrypted("secreto",
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.rows()).isEmpty();
        assertSingleError(parsed.errors(), 0, "file", "ENCRYPTED_FILE", "No se aceptan libros cifrados");
    }

    // ---- lectura: cabeceras -----------------------------------------------------------------------------------

    @Test
    void a_changed_header_is_rejected_naming_the_sheet_and_the_expected_headers() throws Exception {
        byte[] roles = CatalogWorkbooks.xlsx(CatalogWorkbooks.sheet("Roles",
                new String[]{"Nombre", "Alcance*", "Aplicación"}, new String[]{"Auditor", "TENANT", null}));
        byte[] profiles = CatalogWorkbooks.xlsx(CatalogWorkbooks.sheet("Perfiles",
                new String[]{"Nombre de perfil*", "Alcance", "Aplicación"}, new String[]{"Gestores", "TENANT", null}));

        Parsed<CatalogRow> parsedRoles = service.parse("roles", roles);
        Parsed<CatalogRow> parsedProfiles = service.parse("profiles", profiles);

        assertThat(parsedRoles.rows()).isEmpty();
        assertThat(parsedRoles.errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(1);
            assertThat(error.field()).isEqualTo("Roles");
            assertThat(error.code()).isEqualTo("INVALID_HEADERS");
            assertThat(error.description()).startsWith("Los encabezados de la hoja Roles deben ser:")
                    .contains("Nombre de rol*").contains("Alcance*").contains("Aplicación");
        });
        assertThat(parsedProfiles.rows()).isEmpty();
        assertThat(parsedProfiles.errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(1);
            assertThat(error.field()).isEqualTo("Perfiles");
            assertThat(error.code()).isEqualTo("INVALID_HEADERS");
            assertThat(error.description()).startsWith("Los encabezados de la hoja Perfiles deben ser:")
                    .contains("Nombre de perfil*");
        });
    }

    @Test
    void headers_are_case_sensitive_and_a_missing_header_cell_is_rejected() throws Exception {
        byte[] lowerCase = CatalogWorkbooks.xlsx(CatalogWorkbooks.sheet("Roles",
                new String[]{"nombre de rol*", "Alcance*", "Aplicación"}, new String[]{"Auditor", "TENANT", null}));
        byte[] shortHeader = CatalogWorkbooks.xlsx(CatalogWorkbooks.sheet("Roles",
                new String[]{"Nombre de rol*", "Alcance*"}, new String[]{"Auditor", "TENANT", null}));

        assertThat(service.parse("roles", lowerCase).errors()).extracting(BulkResourceRowError::code)
                .containsExactly("INVALID_HEADERS");
        assertThat(service.parse("roles", shortHeader).errors()).extracting(BulkResourceRowError::code)
                .containsExactly("INVALID_HEADERS");
    }

    @Test
    void a_first_data_row_without_a_header_row_is_rejected_instead_of_silently_lost() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.sheet("Roles",
                new String[]{"Auditor", "TENANT", ""}, new String[]{"Editor", "TENANT", ""}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field, BulkResourceRowError::code)
                .containsExactly(tuple(1, "Roles", "INVALID_HEADERS"));
    }

    @Test
    void an_invalid_header_only_in_a_relation_sheet_does_not_stop_the_main_sheet() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.sheet("RolesxRecurso", new String[]{"Rol", "Alcance*", "Aplicación*", "Ruta*", "Método HTTP*"},
                        new String[]{"Auditor", "TENANT", "Mi aplicación", "/api/orders", "GET"}),
                CatalogWorkbooks.sheet("PerfilesxRol", new String[]{"Perfil", "Alcance del perfil*"},
                        new String[]{"Gestores", "TENANT"}));

        Parsed<RoleResourceRow> roleResources = service.parseRoleResources(file);
        Parsed<ProfileRoleRow> profileRoles = service.parseProfileRoles(file);

        assertThat(service.parse("roles", file).errors()).isEmpty();
        assertThat(roleResources.rows()).isEmpty();
        assertThat(roleResources.errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(1);
            assertThat(error.field()).isEqualTo("RolesxRecurso");
            assertThat(error.code()).isEqualTo("INVALID_HEADERS");
            assertThat(error.description()).startsWith("Los encabezados de la hoja RolesxRecurso deben ser:");
        });
        assertThat(profileRoles.rows()).isEmpty();
        assertThat(profileRoles.errors()).extracting(BulkResourceRowError::field, BulkResourceRowError::code)
                .containsExactly(tuple("PerfilesxRol", "INVALID_HEADERS"));
    }

    // ---- lectura: tope de filas -------------------------------------------------------------------------------

    @Test
    void more_than_the_row_limit_reports_too_many_rows_and_reads_only_the_first_thousand() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                CatalogWorkbooks.tenantRoleRows("Rol", BulkCatalogWorkbookService.MAX_ROWS + 1)));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.rows()).hasSize(BulkCatalogWorkbookService.MAX_ROWS);
        assertThat(parsed.rows().getLast().name()).isEqualTo("Rol-1000");
        assertSingleError(parsed.errors(), 0, "Roles", "TOO_MANY_ROWS", "La hoja Roles supera las 1.000 filas de datos");
    }

    @Test
    void exactly_the_row_limit_is_accepted() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(
                CatalogWorkbooks.tenantRoleRows("Perfil", BulkCatalogWorkbookService.MAX_ROWS)));

        Parsed<CatalogRow> parsed = service.parse("profiles", file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).hasSize(BulkCatalogWorkbookService.MAX_ROWS);
    }

    @Test
    void a_relation_sheet_over_the_row_limit_is_truncated_and_reported_under_its_own_name() throws Exception {
        String[][] relations = new String[BulkCatalogWorkbookService.MAX_ROWS + 1][];
        for (int i = 0; i < relations.length; i++) {
            relations[i] = new String[]{"Auditor", "TENANT", "Mi aplicación", "/api/r" + i, "GET"};
        }
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(relations));

        Parsed<RoleResourceRow> parsed = service.parseRoleResources(file);

        assertThat(parsed.rows()).hasSize(BulkCatalogWorkbookService.MAX_ROWS);
        assertSingleError(parsed.errors(), 0, "RolesxRecurso", "TOO_MANY_ROWS",
                "La hoja RolesxRecurso supera las 1.000 filas de datos");
    }

    // ---- lectura: tipos de celda ------------------------------------------------------------------------------

    @Test
    void a_numeric_or_boolean_cell_is_an_error_of_its_own_row_and_the_row_is_kept() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(
                new Object[]{12345, "TENANT", null},
                new Object[]{"Auditor", true, null},
                new String[]{"Editor", "TENANT", null}));

        Parsed<CatalogRow> parsed = service.parse("roles", file);

        assertThat(parsed.rows()).containsExactly(
                new CatalogRow(2, "", "TENANT", ""),
                new CatalogRow(3, "Auditor", "", ""),
                new CatalogRow(4, "Editor", "TENANT", ""));
        assertThat(parsed.errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field,
                        BulkResourceRowError::code, BulkResourceRowError::description)
                .containsExactly(
                        tuple(2, "Nombre de rol*", "UNEXPECTED_CELL_TYPE", "La celda debe contener texto"),
                        tuple(3, "Alcance*", "UNEXPECTED_CELL_TYPE", "La celda debe contener texto"));
    }

    @Test
    void a_formula_or_an_error_cell_is_an_unsupported_cell_of_its_own_row() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(
                new Object[]{"Gestores", "TENANT", new CatalogWorkbooks.Formula("1+1")},
                new Object[]{"Auditores", "TENANT", CatalogWorkbooks.ERROR_CELL}));

        Parsed<CatalogRow> parsed = service.parse("profiles", file);

        assertThat(parsed.rows()).containsExactly(
                new CatalogRow(2, "Gestores", "TENANT", ""),
                new CatalogRow(3, "Auditores", "TENANT", ""));
        assertThat(parsed.errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field,
                        BulkResourceRowError::code, BulkResourceRowError::description)
                .containsExactly(
                        tuple(2, "Aplicación", "UNSUPPORTED_CELL", "No se admiten fórmulas ni celdas de error"),
                        tuple(3, "Aplicación", "UNSUPPORTED_CELL", "No se admiten fórmulas ni celdas de error"));
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

        Parsed<RoleResourceRow> parsed = service.parseRoleResources(file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).containsExactly(
                new RoleResourceRow(2, "Auditor", "TENANT", "Mi aplicación", "/api/orders", "GET"),
                new RoleResourceRow(4, "Editor", "APPLICATION", "Mi aplicación", "/api/items", "POST"));
    }

    @Test
    void a_missing_role_resource_sheet_means_no_relations() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        Parsed<RoleResourceRow> parsed = service.parseRoleResources(file);

        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.errors()).isEmpty();
    }

    @Test
    void a_broken_file_reports_an_error_instead_of_providing_role_resource_relations() {
        Parsed<RoleResourceRow> parsed = service.parseRoleResources(new byte[]{1, 2, 3});

        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.errors()).singleElement()
                .satisfies(error -> assertThat(error.code()).isEqualTo("INVALID_WORKBOOK"));
    }

    @Test
    void a_non_text_cell_in_a_relation_sheet_is_reported_under_the_sheet_name_and_the_row_is_kept() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}),
                CatalogWorkbooks.roleResources(
                        new Object[]{"Auditor", "TENANT", "Mi aplicación", 404, "GET"},
                        new Object[]{"Auditor", "TENANT", "Mi aplicación", "/api/orders", new CatalogWorkbooks.Formula("1+1")}));

        Parsed<RoleResourceRow> parsed = service.parseRoleResources(file);

        assertThat(parsed.rows()).containsExactly(
                new RoleResourceRow(2, "Auditor", "TENANT", "Mi aplicación", "", "GET"),
                new RoleResourceRow(3, "Auditor", "TENANT", "Mi aplicación", "/api/orders", ""));
        assertThat(parsed.errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field, BulkResourceRowError::code)
                .containsExactly(
                        tuple(2, "RolesxRecurso", "UNEXPECTED_CELL_TYPE"),
                        tuple(3, "RolesxRecurso", "UNSUPPORTED_CELL"));
    }

    @Test
    void parses_profile_role_relations_skipping_blank_rows() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(
                        new String[]{"Gestores", "TENANT", null, "Auditor", "APPLICATION", "Mi aplicación"},
                        new String[]{null, null, null, null, null, null},
                        new String[]{"Gestores", "TENANT", "", "Editor", "TENANT", ""}));

        Parsed<ProfileRoleRow> parsed = service.parseProfileRoles(file);

        assertThat(parsed.errors()).isEmpty();
        assertThat(parsed.rows()).containsExactly(
                new ProfileRoleRow(2, "Gestores", "TENANT", "", "Auditor", "APPLICATION", "Mi aplicación"),
                new ProfileRoleRow(4, "Gestores", "TENANT", "", "Editor", "TENANT", ""));
    }

    @Test
    void a_missing_profile_role_sheet_means_no_relations() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}));

        Parsed<ProfileRoleRow> parsed = service.parseProfileRoles(file);

        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.errors()).isEmpty();
    }

    @Test
    void a_broken_file_reports_an_error_instead_of_providing_profile_role_relations() {
        Parsed<ProfileRoleRow> parsed = service.parseProfileRoles(new byte[]{1, 2, 3});

        assertThat(parsed.rows()).isEmpty();
        assertThat(parsed.errors()).singleElement()
                .satisfies(error -> assertThat(error.code()).isEqualTo("INVALID_WORKBOOK"));
    }

    @Test
    void a_non_text_cell_in_the_profile_role_sheet_is_reported_under_the_sheet_name() throws Exception {
        byte[] file = CatalogWorkbooks.xlsx(
                CatalogWorkbooks.profiles(new String[]{"Gestores", "TENANT", null}),
                CatalogWorkbooks.profileRoles(new Object[]{"Gestores", "TENANT", null, 99, "TENANT", null}));

        Parsed<ProfileRoleRow> parsed = service.parseProfileRoles(file);

        assertThat(parsed.rows()).containsExactly(new ProfileRoleRow(2, "Gestores", "TENANT", "", "", "TENANT", ""));
        assertThat(parsed.errors()).extracting(BulkResourceRowError::row, BulkResourceRowError::field, BulkResourceRowError::code)
                .containsExactly(tuple(2, "PerfilesxRol", "UNEXPECTED_CELL_TYPE"));
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
    void the_error_report_of_an_unreadable_file_is_a_workbook_with_only_the_errors_sheet() throws Exception {
        List<BulkResourceRowError> errors = List.of(
                new BulkResourceRowError(0, "file", "", "INVALID_WORKBOOK", "El archivo no es un libro Excel válido"));
        byte[] encrypted = CatalogWorkbooks.encrypted("secreto", CatalogWorkbooks.roles(new String[]{"Auditor", "TENANT", null}));

        for (byte[] unreadable : new byte[][]{{1, 2, 3}, new byte[0], null, encrypted}) {
            try (Workbook report = open(service.errorReport(unreadable, errors))) {
                assertThat(sheetNames(report)).containsExactly("Errores");
                Sheet sheet = report.getSheet("Errores");
                assertThat(headers(sheet)).containsExactly("Fila", "Campo / hoja", "Código", "Descripción");
                assertThat(sheet.getLastRowNum()).isEqualTo(1);
                assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("INVALID_WORKBOOK");
            }
        }
    }
}
