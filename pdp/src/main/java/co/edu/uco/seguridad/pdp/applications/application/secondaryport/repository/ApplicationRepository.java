package co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository;

import co.edu.uco.seguridad.pdp.applications.domain.Application;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
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
     * Consulta por criterio y ventana. Sustituye a los métodos por combinación de filtros: añadir un
     * filtro nuevo es ampliar {@link ApplicationCriteria}, no añadir un método aquí.
     */
    Mono<ResultPage<Application>> findBy(ApplicationCriteria criteria, PageWindow window);

    Mono<Application> save(Application application);

    Mono<Void> deleteById(ApplicationId applicationId);
}
