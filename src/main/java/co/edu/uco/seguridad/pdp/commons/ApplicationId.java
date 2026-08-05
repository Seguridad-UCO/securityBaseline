package co.edu.uco.seguridad.pdp.commons;
import java.util.Objects;
import java.util.UUID;
public record ApplicationId(UUID value) { public ApplicationId { Objects.requireNonNull(value, "application id is required"); } }
