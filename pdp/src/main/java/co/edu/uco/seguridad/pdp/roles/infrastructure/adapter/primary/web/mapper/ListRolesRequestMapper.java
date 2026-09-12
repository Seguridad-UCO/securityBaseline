package co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.roles.application.primaryport.request.ListRolesRequest;
import co.edu.uco.seguridad.pdp.roles.domain.RoleCriteria;
import co.edu.uco.seguridad.pdp.roles.infrastructure.adapter.primary.web.dto.request.raw.ListRolesRawRequest;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;

import java.util.Optional;

/**
 * Resuelve la ventana de paginación (barrera C3): copia exacta de la lógica de
 * ListApplicationsRequestMapper (page/size frente a offset/limit, mezcla ambigua rechazada, rangos
 * comprobados nombrando el campo). El inquilino llega del principal.
 */
public final class ListRolesRequestMapper {

    private static final String FIELD_PAGE = "page";
    private static final String FIELD_SIZE = "size";
    private static final String FIELD_OFFSET = "offset";
    private static final String FIELD_LIMIT = "limit";

    private ListRolesRequestMapper() {
    }

    public static ListRolesRequest toRequest(ListRolesRawRequest raw, TenantId tenantId) {
        return new ListRolesRequest(RoleCriteria.ofTenant(tenantId), window(raw));
    }

    private static PageWindow window(ListRolesRawRequest raw) {
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
