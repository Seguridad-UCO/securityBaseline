package co.edu.uco.seguridad.applications.domain;

public record ResourceIdentifier(String value) {
    public ResourceIdentifier {
        if (value == null || value.isBlank()) throw new DomainException("Initial resource is required");
        value = value.trim();
        if (!value.startsWith("/") || value.length() > 200) {
            throw new DomainException("Initial resource must be an absolute logical path of at most 200 characters");
        }
    }
}
