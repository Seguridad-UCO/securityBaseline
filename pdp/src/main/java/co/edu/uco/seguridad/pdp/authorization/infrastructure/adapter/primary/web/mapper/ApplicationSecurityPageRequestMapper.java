package co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.mapper;

import co.edu.uco.seguridad.pdp.authorization.infrastructure.adapter.primary.web.dto.request.raw.ApplicationSecurityPageRawRequest;
import co.edu.uco.seguridad.pdp.commons.message.ValueObjectMessages;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;
import co.edu.uco.seguridad.shared.web.message.WebContractMessages;

import java.util.Optional;

/** Única traducción HTTP de ambos estilos de página para el slice de seguridad de aplicación. */
public final class ApplicationSecurityPageRequestMapper {
    private ApplicationSecurityPageRequestMapper() { }

    public static PageWindow toWindow(ApplicationSecurityPageRawRequest raw) {
        Optional<String> page = RequestFieldParser.optional(raw.page());
        Optional<String> size = RequestFieldParser.optional(raw.size());
        Optional<String> offset = RequestFieldParser.optional(raw.offset());
        Optional<String> limit = RequestFieldParser.optional(raw.limit());
        if ((page.isPresent() || size.isPresent()) && (offset.isPresent() || limit.isPresent())) {
            throw new ConflictingRequestParametersException("page", WebContractMessages.conflictingPagingModes());
        }
        if (page.isPresent() || size.isPresent()) return PageWindow.ofPage(nonNegative("page", page, ValueObjectMessages.PageWindow.NEGATIVE_PAGE), sized("size", size, ValueObjectMessages.PageWindow.SIZE_RANGE));
        if (offset.isPresent() || limit.isPresent()) return PageWindow.ofRange(nonNegative("offset", offset, ValueObjectMessages.PageWindow.NEGATIVE_OFFSET), sized("limit", limit, ValueObjectMessages.PageWindow.LIMIT_RANGE));
        return PageWindow.defaultWindow();
    }

    private static int nonNegative(String field, Optional<String> raw, String message) {
        int value = raw.map(v -> RequestFieldParser.parseInt(field, v)).orElse(0);
        if (value < 0) throw new MalformedRequestFieldException(field, message);
        return value;
    }
    private static int sized(String field, Optional<String> raw, String message) {
        int value = raw.map(v -> RequestFieldParser.parseInt(field, v)).orElse(PageWindow.DEFAULT_LIMIT);
        if (value < 1 || value > PageWindow.MAX_LIMIT) throw new MalformedRequestFieldException(field, message);
        return value;
    }
}
