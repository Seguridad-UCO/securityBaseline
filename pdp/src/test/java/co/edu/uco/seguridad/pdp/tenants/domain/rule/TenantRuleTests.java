package co.edu.uco.seguridad.pdp.tenants.domain.rule;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.DuplicateTenantException;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotActiveException;
import co.edu.uco.seguridad.pdp.tenants.domain.exception.TenantNotFoundException;
import co.edu.uco.seguridad.pdp.tenants.domain.model.TenantStatus;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantCodeMustBeUniqueRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantMustExistRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.impl.TenantStatusMustBeActiveRuleImpl;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantActivation;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantCodeAvailability;
import co.edu.uco.seguridad.pdp.tenants.domain.rule.model.TenantExistence;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Las tres reglas del módulo, cada una por separado. Ninguna consulta nada: reciben el dato ya
 * resuelto, así que la prueba es una llamada a método — sin StepVerifier y sin repositorio falso.
 */
class TenantRuleTests {

    private static final TenantId TENANT = new TenantId("universidad-uco");

    @Test
    void existence_rule_accepts_a_registered_tenant() {
        TenantMustExistRule rule = new TenantMustExistRuleImpl();

        assertThatCode(() -> rule.execute(new TenantExistence(TENANT, true))).doesNotThrowAnyException();
    }

    @Test
    void existence_rule_refuses_an_unknown_tenant() {
        TenantMustExistRule rule = new TenantMustExistRuleImpl();

        assertThatThrownBy(() -> rule.execute(new TenantExistence(TENANT, false)))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("universidad-uco");
    }

    @Test
    void status_rule_accepts_an_active_tenant() {
        TenantStatusMustBeActiveRule rule = new TenantStatusMustBeActiveRuleImpl();

        assertThatCode(() -> rule.execute(new TenantActivation(TENANT, TenantStatus.ACTIVE)))
                .doesNotThrowAnyException();
    }

    @Test
    void status_rule_refuses_a_suspended_tenant() {
        TenantStatusMustBeActiveRule rule = new TenantStatusMustBeActiveRuleImpl();

        assertThatThrownBy(() -> rule.execute(new TenantActivation(TENANT, TenantStatus.SUSPENDED)))
                .isInstanceOf(TenantNotActiveException.class)
                .hasMessageContaining("SUSPENDED");
    }

    @Test
    void uniqueness_rule_accepts_a_free_identifier() {
        TenantCodeMustBeUniqueRule rule = new TenantCodeMustBeUniqueRuleImpl();

        assertThatCode(() -> rule.execute(new TenantCodeAvailability(TENANT, false))).doesNotThrowAnyException();
    }

    @Test
    void uniqueness_rule_refuses_an_identifier_already_taken() {
        TenantCodeMustBeUniqueRule rule = new TenantCodeMustBeUniqueRuleImpl();

        assertThatThrownBy(() -> rule.execute(new TenantCodeAvailability(TENANT, true)))
                .isInstanceOf(DuplicateTenantException.class);
    }
}
