package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.port.primary.dto.request.ListApplicationsRequest;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.commons.TenantId;

/**
 * Traduce los parámetros crudos de la consulta al request tipado del núcleo.
 *
 * <p>Aquí vive la resolución de la ventana: sin parámetros se usa la ventana por defecto;
 * {@code page}/{@code size} y {@code offset}/{@code limit} son dos formas equivalentes de pedir lo
 * mismo, y mezclarlas es ambiguo, así que se rechaza en vez de adivinar.
 *
 * <p>El inquilino no se lee de la consulta: lo aporta el interactor desde el principal autenticado.
 *
 * <p>Esqueleto de la SPEC de HU-001 — sin lógica todavía.
 */
public final class ListApplicationsRequestMapper {

    private ListApplicationsRequestMapper() {
    }

    public static ListApplicationsRequest toRequest(ListApplicationsRawRequest raw, TenantId tenantId) {
        throw new UnsupportedOperationException("pendiente: HU-001");
    }
}
