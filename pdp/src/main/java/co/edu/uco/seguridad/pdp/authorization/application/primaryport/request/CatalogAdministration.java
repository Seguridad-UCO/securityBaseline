package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;

import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;

import java.util.Objects;

/**
 * Qué gate exige una escritura de catálogo (roles, perfiles y sus asociaciones) según el alcance de su
 * objetivo (HU-036). Reemplaza al {@code Optional<AdministrationRequest>} de HU-016/HU-019, donde
 * «vacío» significaba «no se exige nada» y dejaba sin dueño el alcance {@code TENANT}: ahora no hay
 * estado «sin gate» — o se administra la aplicación del objetivo, o se administra al menos una
 * aplicación del inquilino.
 */
public sealed interface CatalogAdministration {

    /** Objetivo de alcance {@code APPLICATION}: se exige administrar esa aplicación (ADR-023). */
    record OfApplication(AdministrationRequest administration) implements CatalogAdministration {

        public OfApplication {
            Objects.requireNonNull(administration, RequiredArgumentMessages.ADMINISTRATION);
        }
    }

    /** Objetivo de alcance {@code TENANT}: se exige administrar al menos una aplicación del inquilino (ADR-036). */
    record OfTenant(TenantAdministrationRequest actor) implements CatalogAdministration {

        public OfTenant {
            Objects.requireNonNull(actor, RequiredArgumentMessages.TENANT_ADMINISTRATION_REQUEST);
        }
    }
}
