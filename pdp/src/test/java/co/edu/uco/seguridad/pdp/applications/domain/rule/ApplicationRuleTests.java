package co.edu.uco.seguridad.pdp.applications.domain.rule;

import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationExistence;
import co.edu.uco.seguridad.pdp.applications.domain.rule.model.ApplicationNameAvailability;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ApplicationNotFoundException;
import co.edu.uco.seguridad.pdp.applications.domain.exception.DuplicateApplicationException;
import co.edu.uco.seguridad.pdp.applications.domain.exception.ReservedApplicationNameException;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationMustExistForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationNameMustBeUniqueForTenantRuleImpl;
import co.edu.uco.seguridad.pdp.applications.domain.rule.impl.ApplicationNameMustNotBeReservedRuleImpl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationName;
import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Las tres reglas del módulo, puras: reciben el dato ya resuelto y solo deciden. */
class ApplicationRuleTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");
    private static final ApplicationId ID = new ApplicationId(UUID.randomUUID());
    private static final ApplicationName NAME = new ApplicationName("gestion-academica");

    @Test
    void reserved_name_rule_refuses_a_platform_name_whatever_its_case() {
        ApplicationNameMustNotBeReservedRule rule =
                new ApplicationNameMustNotBeReservedRuleImpl(Set.of("admin", "pdp"));

        assertThatThrownBy(() -> rule.execute(new ApplicationName("Admin")))
                .isInstanceOf(ReservedApplicationNameException.class)
                .hasMessageContaining("reservado");
    }

    @Test
    void reserved_name_rule_allows_anything_else() {
        ApplicationNameMustNotBeReservedRule rule = new ApplicationNameMustNotBeReservedRuleImpl(Set.of("admin"));

        assertThatCode(() -> rule.execute(NAME)).doesNotThrowAnyException();
    }

    @Test
    void uniqueness_rule_accepts_a_name_the_tenant_has_not_used() {
        ApplicationNameMustBeUniqueForTenantRule rule = new ApplicationNameMustBeUniqueForTenantRuleImpl();

        assertThatCode(() -> rule.execute(new ApplicationNameAvailability(TENANT, NAME, false)))
                .doesNotThrowAnyException();
    }

    @Test
    void uniqueness_rule_refuses_a_name_the_tenant_already_registered() {
        ApplicationNameMustBeUniqueForTenantRule rule = new ApplicationNameMustBeUniqueForTenantRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ApplicationNameAvailability(TENANT, NAME, true)))
                .isInstanceOf(DuplicateApplicationException.class)
                .hasMessageContaining("gestion-academica");
    }

    @Test
    void existence_rule_accepts_an_application_the_tenant_owns() {
        ApplicationMustExistForTenantRule rule = new ApplicationMustExistForTenantRuleImpl();

        assertThatCode(() -> rule.execute(new ApplicationExistence(TENANT, ID, true))).doesNotThrowAnyException();
    }

    @Test
    void existence_rule_refuses_an_application_the_tenant_does_not_own() {
        ApplicationMustExistForTenantRule rule = new ApplicationMustExistForTenantRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ApplicationExistence(TENANT, ID, false)))
                .isInstanceOf(ApplicationNotFoundException.class)
                .hasMessageContaining(ID.value().toString());
    }
}
