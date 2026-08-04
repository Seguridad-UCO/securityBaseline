package co.edu.uco.seguridad.applications.domain;

public record ApplicationName(String value) {
    public ApplicationName {
        if (value == null || value.isBlank()) throw new DomainException("Application name is required");
        value = value.trim();
        if (value.length() < 3 || value.length() > 100) {
            throw new DomainException("Application name must contain 3 to 100 characters");
        }
    }
}
