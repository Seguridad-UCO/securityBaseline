package co.edu.uco.seguridad.pdp.commons;
import java.util.Objects;
import java.util.UUID;
public record ResourceId(UUID value) { public ResourceId { Objects.requireNonNull(value, "resource id is required"); } }
