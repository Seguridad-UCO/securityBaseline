package co.edu.uco.seguridad.applications.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Immutable boundary DTO. The domain performs its own invariant validation afterwards. */
public record RegisterProtectedApplicationRequest(
    @NotBlank @Size(min = 3, max = 100) String name,
    @NotBlank @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9_-]{1,63}") String tenantId,
    @NotBlank @Size(max = 200) @Pattern(regexp = "/.*") String resource) { }
