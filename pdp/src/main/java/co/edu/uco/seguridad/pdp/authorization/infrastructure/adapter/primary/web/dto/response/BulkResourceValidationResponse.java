package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.response;

import java.util.List;

/** Diagnóstico no persistente de un archivo de carga masiva. */
public record BulkResourceValidationResponse(boolean ready, int totalRows, int validRows, int invalidRows,
                                             List<BulkResourceRowError> errors) {
    public record BulkResourceRowError(int row, String field, String value, String code, String description) { }
}
