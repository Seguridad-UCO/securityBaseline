package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;
import java.util.Optional;

/**
 * Specification de consulta de aplicaciones protegidas: expresa <em>qué</em> se busca sin decir
 * <em>cómo</em> se ejecuta. El adaptador de persistencia la traduce a una consulta; {@link #matches}
 * permite probarla aislada y que cualquier adaptador en memoria respete la misma decisión.
 *
 * <p>El inquilino es obligatorio a propósito: el aislamiento entre inquilinos no es una regla que
 * pueda olvidarse, es una precondición que el compilador exige.
 *
 * <p><strong>Por qué {@code Optional} como componente.</strong> La objeción habitual a
 * {@code Optional} en un campo es la serialización, y este objeto no se serializa nunca: no sale por
 * HTTP ni se persiste. La alternativa —un {@code String} anulable con un accessor que devuelva
 * {@code Optional}— obligaría a sobrescribir el accessor generado, dejando {@code equals} y
 * {@code hashCode} operando sobre el campo mientras el accessor anuncia otro tipo. Y usar
 * {@code ""} como «sin filtro» confundiría ausente con vacío, que es justo lo que hay que
 * distinguir. Es una decisión consciente: no la «corrijas» a un campo anulable.
 */
public record ApplicationCriteria(TenantId tenantId, Optional<String> nameContains) {

    public ApplicationCriteria {
        Objects.requireNonNull(tenantId, RequiredArgumentMessages.TENANT_ID);
        Objects.requireNonNull(nameContains, RequiredArgumentMessages.NAME_FILTER);
    }

    public static ApplicationCriteria of(TenantId tenantId, Optional<String> nameContains) {
        return new ApplicationCriteria(tenantId, nameContains);
    }

    /** Todas las aplicaciones del inquilino, sin filtrar por nombre. */
    public static ApplicationCriteria ofTenant(TenantId tenantId) {
        return new ApplicationCriteria(tenantId, Optional.empty());
    }

    /**
     * Decide si una aplicación satisface el criterio. El inquilino se comprueba siempre; el nombre,
     * solo si hay filtro, y delegando la comparación al propio value object.
     */
    public boolean matches(Application application) {
        Objects.requireNonNull(application, RequiredArgumentMessages.APPLICATION_NAME);
        if (!application.tenantId().equals(tenantId)) {
            return false;
        }
        return nameContains.map(fragment -> application.name().contains(fragment)).orElse(true);
    }
}
