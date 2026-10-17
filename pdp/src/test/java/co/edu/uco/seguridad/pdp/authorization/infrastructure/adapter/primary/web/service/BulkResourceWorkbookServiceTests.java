package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.service;

import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import static org.assertj.core.api.Assertions.assertThat;

class BulkResourceWorkbookServiceTests {
    private final BulkResourceWorkbookService service = new BulkResourceWorkbookService();

    @Test
    void generates_both_template_formats_with_import_sheet_and_http_validation() throws Exception {
        for (boolean xlsx : new boolean[]{true, false}) {
            try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(service.template(xlsx)))) {
                assertThat(workbook.getSheet("Recursos")).isNotNull();
                assertThat(workbook.getSheet("Instrucciones")).isNotNull();
                assertThat(workbook.getSheet("Recursos").getDataValidations()).hasSize(1);
            }
        }
    }

    @Test
    void rejects_empty_input() {
        var result = service.parse(new byte[0]);
        assertThat(result.errors()).extracting(error -> error.code()).contains("EMPTY_FILE");
    }
}
