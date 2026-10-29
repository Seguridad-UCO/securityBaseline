package co.edu.uco.seguridad.pdp.applications.domain.rule.model;

import co.edu.uco.seguridad.pdp.applications.domain.model.ApplicationEnvironment;

import java.util.Set;

/** Ambientes seleccionados para una aplicación, normalizados para que la regla sea pura. */
public record ApplicationEnvironmentSelection(Set<ApplicationEnvironment> enabledEnvironments) {

    public ApplicationEnvironmentSelection {
        enabledEnvironments = enabledEnvironments == null ? Set.of() : Set.copyOf(enabledEnvironments);
    }
}
