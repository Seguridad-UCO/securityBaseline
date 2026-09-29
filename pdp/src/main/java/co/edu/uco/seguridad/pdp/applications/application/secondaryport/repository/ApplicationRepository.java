package co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationCredentialHash;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import reactor.core.publisher.Mono;

/**
 * Puerto secundario para almacenamiento de aplicaciones, expresado solo en tipos de dominio.
 *
 * <p>La existencia es una pregunta de primera clase aquí en lugar de un {@code findBy} que el
 * llamador tendría que interpretar, así que el adaptador puede responderla con un índice en lugar
 * de cargar una fila.</p>
 */
public interface ApplicationRepository {

    Mono<Boolean> existsByTenantAndName(TenantId tenantId, ApplicationName name);

    Mono<Boolean> existsByTenantAndId(TenantId tenantId, ApplicationId applicationId);

    /**
     * Resuelve qué inquilino es dueño de una aplicación, a partir solo de su identificador — a
     * diferencia de {@link #existsByTenantAndId}, que verifica una pertenencia ya conocida. Vacío si
     * la aplicación no existe (HU-003, canal interno del PEP). Responde solo el {@code TenantId},
     * nunca la aplicación completa: es lo mínimo que {@code ApplicationOwnerLookupValidator} necesita.
     */
    Mono<TenantId> findTenantIdById(ApplicationId applicationId);

    /**
     * Resuelve una aplicación desde el nombre que expone el canal interno del PEP. El resultado
     * queda vacío si no existe y falla si el nombre corresponde a más de un tenant: sin un tenant
     * en el contrato no sería seguro elegir una de ellas arbitrariamente.
     */
    default Mono<Application> findUniqueByName(ApplicationName name) {
        return Mono.error(() -> new UnsupportedOperationException("La búsqueda por nombre no está implementada"));
    }

    /**
     * Resuelve el hash guardado de una aplicación, a partir solo de su identificador. Vacío si la
     * aplicación no existe (HU-013, canal interno de validación de credenciales). Responde solo el
     * hash, nunca la aplicación completa.
     */
    Mono<ApplicationCredentialHash> findCredentialHashById(ApplicationId applicationId);

    /**
     * Resuelve el agregado completo por id, solo si pertenece al inquilino indicado. Vacío en
     * cualquier otro caso — no distingue "no existe" de "es de otro inquilino" (HU-014, rotación).
     */
    Mono<Application> findByIdForTenant(TenantId tenantId, ApplicationId applicationId);

    /** Actualiza solo el hash de la credencial — nunca reconstruye el resto de la fila (HU-014). */
    Mono<Void> updateCredentialHash(ApplicationId applicationId, ApplicationCredentialHash credentialHash);

    /**
     * Consulta por criterio y ventana. Sustituye a los métodos por combinación de filtros: añadir un
     * filtro nuevo es ampliar {@link ApplicationCriteria}, no añadir un método aquí.
     */
    Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window);

    Mono<Application> save(Application application);

    default Mono<Application> update(Application application) { return Mono.error(new UnsupportedOperationException()); }

    Mono<Void> deleteById(ApplicationId applicationId);
}
