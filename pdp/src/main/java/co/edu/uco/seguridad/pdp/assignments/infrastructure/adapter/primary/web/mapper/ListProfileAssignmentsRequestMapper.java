package co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ListProfileAssignmentsRequest;
import co.edu.uco.seguridad.pdp.assignments.domain.ProfileAssignmentCriteria;
import co.edu.uco.seguridad.pdp.assignments.infrastructure.adapter.primary.web.dto.request.raw.ListProfileAssignmentsRawRequest;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.pdp.commons.model.ProfileId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;

import java.util.Optional;

/** Misma lógica que ListAssignmentsRequestMapper (barrera C1): copia exacta, sobre profileId en vez de roleId. */
public final class ListProfileAssignmentsRequestMapper {

    private static final String FIELD_PAGE = "page";
    private static final String FIELD_SIZE = "size";
    private static final String FIELD_OFFSET = "offset";
    private static final String FIELD_LIMIT = "limit";

    private ListProfileAssignmentsRequestMapper() {
    }

    public static ListProfileAssignmentsRequest toRequest(ListProfileAssignmentsRawRequest raw, TenantId tenantId) {
        ProfileId profileId = RequestFieldParser.parse("profileId", raw.profileId(), ProfileId::of);
        return new ListProfileAssignmentsRequest(ProfileAssignmentCriteria.of(profileId, tenantId), window(raw));
    }

    private static PageWindow window(ListProfileAssignmentsRawRequest raw) {
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
