package co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity;

import java.util.Objects;
import java.util.UUID;

/** Identificador interno, opaco y no reutilizable de una identidad de servicio. */
public record ServiceIdentityId(UUID value) {
    public ServiceIdentityId { Objects.requireNonNull(value, "service identity id es requerido"); }
    public static ServiceIdentityId of(String value) { return new ServiceIdentityId(UUID.fromString(value)); }
}
