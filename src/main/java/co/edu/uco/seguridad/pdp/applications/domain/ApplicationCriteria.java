package co.edu.uco.seguridad.pdp.applications.domain;

import co.edu.uco.seguridad.pdp.commons.TenantId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

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
 *
 * <p>Esqueleto de la SPEC de HU-001 — sin lógica todavía.
 */
public record ApplicationCriteria(TenantId tenantId, Optional<String> nameContains) {

    public ApplicationCriteria {
        throw new UnsupportedOperationException("pendiente: HU-001");
    }

    public static ApplicationCriteria of(TenantId tenantId, Optional<String> nameContains) {
        throw new UnsupportedOperationException("pendiente: HU-001");
    }

    public static ApplicationCriteria ofTenant(TenantId tenantId) {
        throw new UnsupportedOperationException("pendiente: HU-001");
    }

    /**
     * Decide si una aplicación satisface el criterio: mismo inquilino y, si hay filtro de nombre,
     * que el nombre lo contenga sin distinguir mayúsculas.
     */
    public boolean matches(Application application) {
        throw new UnsupportedOperationException("pendiente: HU-001");
    }
}
