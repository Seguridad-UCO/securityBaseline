package co.edu.uco.seguridad.pdp.resources.application.primaryport.request;

import java.util.List;
import java.util.Objects;

/** Lote ya validado por su adaptador; resources conserva la escritura y emisión de eventos. */
public record RegisterProtectedResourcesAtomicallyRequest(List<RegisterProtectedResourceRequest> resources) {
    public RegisterProtectedResourcesAtomicallyRequest {
        resources = List.copyOf(Objects.requireNonNull(resources));
    }
}
