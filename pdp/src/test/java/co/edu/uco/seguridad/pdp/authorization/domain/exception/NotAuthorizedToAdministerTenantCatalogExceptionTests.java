package co.edu.uco.seguridad.pdp.authorization.domain.exception;

import co.edu.uco.seguridad.pdp.authorization.domain.message.AuthorizationMessages;
import co.edu.uco.seguridad.pdp.commons.exception.BusinessRuleViolationException;
import co.edu.uco.seguridad.pdp.commons.exception.ConflictBusinessRuleException;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HU-036: distinta de {@link NotAuthorizedToAdministerException} a propósito — aquí no hay una aplicación
 * que se haya intentado administrar. Lleva la primera aplicación evaluada (si la hubo) para que la
 * auditoría atribuya a ella el evento {@code DENIED}.
 */
class NotAuthorizedToAdministerTenantCatalogExceptionTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.fromString("00000000-0000-0000-0000-0000000000a1"));

    @Test
    void carries_its_own_code_and_the_catalog_message() {
        NotAuthorizedToAdministerTenantCatalogException exception =
                new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.of(APPLICATION));

        assertThat(exception.code()).isEqualTo("NOT_AUTHORIZED_TO_ADMINISTER_TENANT_CATALOG");
        assertThat(exception.getMessage()).isEqualTo(AuthorizationMessages.notAuthorizedToAdministerTenantCatalog());
    }

    @Test
    void is_a_business_rule_violation_and_not_a_conflict() {
        NotAuthorizedToAdministerTenantCatalogException exception =
                new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.empty());

        assertThat(exception).isInstanceOf(BusinessRuleViolationException.class);
        assertThat(exception).isNotInstanceOf(ConflictBusinessRuleException.class);
    }

    @Test
    void exposes_the_evaluated_application_when_there_was_one() {
        NotAuthorizedToAdministerTenantCatalogException exception =
                new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.of(APPLICATION));

        assertThat(exception.evaluatedApplication()).contains(APPLICATION);
    }

    @Test
    void exposes_an_empty_evaluated_application_when_the_subject_had_none() {
        NotAuthorizedToAdministerTenantCatalogException exception =
                new NotAuthorizedToAdministerTenantCatalogException(TENANT, Optional.empty());

        assertThat(exception.evaluatedApplication()).isEmpty();
    }
}
