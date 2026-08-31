package co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ListApplicationsRequest;
import co.edu.uco.seguridad.pdp.applications.domain.ApplicationCriteria;
import co.edu.uco.seguridad.pdp.applications.infrastructure.adapter.primary.web.dto.request.raw.ListApplicationsRawRequest;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;

import java.util.Optional;

/**
 * Traduce los parámetros crudos de la consulta al request tipado del núcleo.
 *
 * <p>Aquí vive la resolución de la ventana: sin parámetros se usa la ventana por defecto;
 * {@code page}/{@code size} y {@code offset}/{@code limit} son dos formas equivalentes de pedir lo
 * mismo, y mezclarlas es ambiguo, así que se rechaza en vez de adivinar.
 *
 * <p>Los rangos se comprueban aquí además de en {@link PageWindow} porque este es el borde HTTP y su
 * obligación es decir <em>qué campo</em> viene mal. No se duplica ni el umbral (sale de
 * {@code PageWindow.MAX_LIMIT}) ni el texto (sale del catálogo de mensajes): el value object sigue
 * siendo la garantía dura, y esto solo añade el nombre del campo al error.
 *
 * <p>El inquilino no se lee de la consulta: lo aporta el interactor desde el principal autenticado.
 */
public final class ListApplicationsRequestMapper {

    private static final String FIELD_PAGE = "page";
    private static final String FIELD_SIZE = "size";
    private static final String FIELD_OFFSET = "offset";
    private static final String FIELD_LIMIT = "limit";

    private ListApplicationsRequestMapper() {
    }

    public static ListApplicationsRequest toRequest(ListApplicationsRawRequest raw, TenantId tenantId) {
        Optional<String> name = RequestFieldParser.optional(raw.name());
        return new ListApplicationsRequest(ApplicationCriteria.of(tenantId, name), window(raw));
    }

    private static PageWindow window(ListApplicationsRawRequest raw) {
        Optional<String> page = RequestFieldParser.optional(raw.page());
        Optional<String> size = RequestFieldParser.optional(raw.size());
        Optional<String> offset = RequestFieldParser.optional(raw.offset());
        Optional<String> limit = RequestFieldParser.optional(raw.limit());

        boolean byPage = page.isPresent() || size.isPresent();
        boolean byRange = offset.isPresent() || limit.isPresent();

        if (byPage && byRange) {
            throw new ConflictingRequestParametersException(FIELD_PAGE, WebContractMessages.conflictingPagingModes());
        }
        if (byPage) {
            return PageWindow.ofPage(
                    notNegative(FIELD_PAGE, page, ValueObjectMessages.PageWindow.NEGATIVE_PAGE),
                    inRange(FIELD_SIZE, size, ValueObjectMessages.PageWindow.SIZE_RANGE));
        }
        if (byRange) {
            return PageWindow.ofRange(
                    notNegative(FIELD_OFFSET, offset, ValueObjectMessages.PageWindow.NEGATIVE_OFFSET),
                    inRange(FIELD_LIMIT, limit, ValueObjectMessages.PageWindow.LIMIT_RANGE));
        }
        return PageWindow.defaultWindow();
    }

    private static int notNegative(String field, Optional<String> raw, String reason) {
        int value = raw.map(present -> RequestFieldParser.parseInt(field, present)).orElse(0);
        if (value < 0) {
            throw new MalformedRequestFieldException(field, reason);
        }
        return value;
    }

    private static int inRange(String field, Optional<String> raw, String reason) {
        int value = raw.map(present -> RequestFieldParser.parseInt(field, present))
                .orElse(PageWindow.DEFAULT_LIMIT);
        if (value < 1 || value > PageWindow.MAX_LIMIT) {
            throw new MalformedRequestFieldException(field, reason);
        }
        return value;
    }
}
