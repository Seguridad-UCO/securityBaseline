package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request;

import co.edu.uco.seguridad.pdp.commons.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.pdp.recursos.domain.ActionCode;
import co.edu.uco.seguridad.pdp.recursos.domain.ResourceCode;

import java.util.Objects;

/**
 * Nivel dos de la estrategia de entrada en dos niveles: la solicitud una vez que se sabe que está bien formada.
 *
 * <p>Este es el único lugar en el código base donde la mutabilidad y los setters son deliberados. Cada
 * setter es el paso de validación para su campo — primero la presencia, luego el objeto de valor, que es
 * lo que convierte un {@code String} en un tipo en el que el resto del sistema puede confiar.</p>
 *
 * <p>Más allá de esta clase ningún {@code String} sin procesar viaja hacia adentro, por lo que el dominio no
 * puede alcanzarse con entrada sin analizar. El objeto escapa del mapper solo después de que cada setter
 * se ha ejecutado.</p>
 */
public final class RegisterProtectedApplicationRequest {

    private TenantId tenantId;
    private ApplicationName applicationName;
    private ResourceCode resourceCode;
    private ActionCode action;

    public void setTenantId(String value) {
        this.tenantId = RequestFieldParser.parse("tenantId", value, TenantId::new);
    }

    public void setApplicationName(String value) {
        this.applicationName = RequestFieldParser.parse("applicationName", value, ApplicationName::new);
    }

    public void setResourceCode(String value) {
        this.resourceCode = RequestFieldParser.parse("resourceCode", value, ResourceCode::new);
    }

    public void setAction(String value) {
        this.action = RequestFieldParser.parse("action", value, ActionCode::new);
    }

    public TenantId tenantId() {
        return required("tenantId", tenantId);
    }

    public ApplicationName applicationName() {
        return required("applicationName", applicationName);
    }

    public ResourceCode resourceCode() {
        return required("resourceCode", resourceCode);
    }

    public ActionCode action() {
        return required("action", action);
    }

    private static <T> T required(String field, T value) {
        return Objects.requireNonNull(value, () -> "field '" + field + "' was never set");
    }
}
