package co.edu.uco.seguridad.pdp.recursos.infrastructure.adapter.primary.web.dto.request;

import co.edu.uco.seguridad.crosscutting.messages.WebContractMessages;
import co.edu.uco.seguridad.pdp.commons.PageWindow;
import co.edu.uco.seguridad.pdp.commons.exception.InvalidValueException;
import co.edu.uco.seguridad.shared.web.RequestFieldParser;
import co.edu.uco.seguridad.shared.web.exception.ConflictingRequestParametersException;
import co.edu.uco.seguridad.shared.web.exception.MalformedRequestFieldException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Nivel dos para la consulta de catálogo: filtros ya analizados y una ventana acotada. Un solo setter
 * para los cuatro parámetros de ventana porque paginación e intervalos son la misma cosa, y su validez
 * depende de la combinación, no de un valor individual.
 */
public final class SearchProtectedApplicationsRequest {

    private Optional<String> nameContains = Optional.empty();
    private Optional<String> resourceContains = Optional.empty();
    private PageWindow window = PageWindow.defaultWindow();

    public void setNameContains(String value) {
        this.nameContains = RequestFieldParser.optional(value);
    }

    public void setResourceContains(String value) {
        this.resourceContains = RequestFieldParser.optional(value);
    }

    /**
     * Acepta {@code page}/{@code size}, o {@code offset}/{@code limit}, o ninguno. Mezclar los
     * dos estilos se rechaza en lugar de favorecer uno silenciosamente.
     */
    public void setResultWindow(String page, String size, String offset, String limit) {
        boolean pagingRequested = isPresent(page) || isPresent(size);
        boolean rangeRequested = isPresent(offset) || isPresent(limit);

        if (pagingRequested && rangeRequested) {
            throw new ConflictingRequestParametersException("page",
                    WebContractMessages.conflictingPagingModes());
        }
        if (rangeRequested) {
            this.window = rangeWindow(offset, limit);
        } else if (pagingRequested) {
            this.window = pageWindow(page, size);
        }
    }

    public Optional<String> nameContains() {
        return nameContains;
    }

    public Optional<String> resourceContains() {
        return resourceContains;
    }

    public PageWindow window() {
        return window;
    }

    private static PageWindow rangeWindow(String offset, String limit) {
        if (!isPresent(offset) || !isPresent(limit)) {
            throw new ConflictingRequestParametersException("limit",
                    WebContractMessages.offsetLimitTogether());
        }
        return build("limit", () -> PageWindow.ofRange(
                RequestFieldParser.parseInt("offset", offset),
                RequestFieldParser.parseInt("limit", limit)));
    }

    private static PageWindow pageWindow(String page, String size) {
        int resolvedPage = isPresent(page) ? RequestFieldParser.parseInt("page", page) : 0;
        int resolvedSize = isPresent(size) ? RequestFieldParser.parseInt("size", size) : PageWindow.DEFAULT_LIMIT;
        return build("size", () -> PageWindow.ofPage(resolvedPage, resolvedSize));
    }

    private static PageWindow build(String field, Supplier<PageWindow> factory) {
        try {
            return factory.get();
        } catch (InvalidValueException cause) {
            throw new MalformedRequestFieldException(field, cause.getMessage());
        }
    }

    private static boolean isPresent(String value) {
        return RequestFieldParser.optional(value).isPresent();
    }
}
