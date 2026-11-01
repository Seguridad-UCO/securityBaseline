package co.edu.uco.seguridad.pdp.authorization.domain.serviceidentity;

/** Rol técnico mínimo exigido para que un grant M2M proyecte un entitlement en el PDP. */
public enum ServiceTechnicalRole {
    SERVICE_CONSUMER;

    public static ServiceTechnicalRole from(String value) {
        try {
            return ServiceTechnicalRole.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (RuntimeException error) {
            throw new IllegalArgumentException("rol técnico de servicio inválido");
        }
    }
}
