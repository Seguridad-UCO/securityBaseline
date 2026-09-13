package co.edu.uco.seguridad.pdp.profiles.domain.rule.impl;

import co.edu.uco.seguridad.pdp.profiles.domain.exception.DuplicateProfileNameException;
import co.edu.uco.seguridad.pdp.profiles.domain.model.ProfileName;
import co.edu.uco.seguridad.pdp.profiles.domain.rule.model.ProfileNameAvailability;
import co.edu.uco.seguridad.pdp.roles.domain.model.RoleScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProfileNameMustBeUniqueInScopeRuleImplTests {

    private static final RoleScope SCOPE = RoleScope.global();

    // ProfileName es un esqueleto que aún lanza en su constructor compacto (pendiente: HU-011): se
    // construye dentro de cada @Test, nunca como campo estático, para que el rojo quede localizado
    // en el caso que corresponde y no tumbe la clase entera al cargarla.
    @Test
    void rejects_a_name_already_taken_in_that_scope() {
        ProfileNameMustBeUniqueInScopeRuleImpl rule = new ProfileNameMustBeUniqueInScopeRuleImpl();

        assertThatThrownBy(() -> rule.execute(new ProfileNameAvailability(new ProfileName("Coordinador académico"), SCOPE, true)))
                .isInstanceOf(DuplicateProfileNameException.class);
    }

    @Test
    void accepts_a_name_that_is_free_in_that_scope() {
        ProfileNameMustBeUniqueInScopeRuleImpl rule = new ProfileNameMustBeUniqueInScopeRuleImpl();

        assertThatCode(() -> rule.execute(new ProfileNameAvailability(new ProfileName("Coordinador académico"), SCOPE, false)))
                .doesNotThrowAnyException();
    }
}
