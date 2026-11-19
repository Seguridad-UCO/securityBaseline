package co.edu.uco.seguridad.pdp.authorization.application.rule.validator;

import co.edu.uco.seguridad.pdp.assignments.application.primaryport.request.ResolveActiveRolesRequest;
import co.edu.uco.seguridad.pdp.assignments.application.primaryport.response.ActiveRolesResponse;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.impl.ActiveRoleNamesLookupValidatorImpl;
import co.edu.uco.seguridad.pdp.commons.model.ApplicationId;
import co.edu.uco.seguridad.pdp.commons.model.RoleId;
import co.edu.uco.seguridad.pdp.commons.model.UserId;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Orquesta assignments.ResolveActiveRolesUseCase y roles.RoleNamesLookupValidator, en ese orden.
 */
class ActiveRoleNamesLookupValidatorImplTests {

    private static final UserId USER_ID = new UserId(UUID.randomUUID());
    private static final ApplicationId APPLICATION = new ApplicationId(UUID.randomUUID());
    private static final RoleId ROLE_ID = new RoleId(UUID.randomUUID());
    private static final ResolveActiveRolesRequest REQUEST = new ResolveActiveRolesRequest(USER_ID, APPLICATION);

    @Test
    void chains_the_resolved_role_ids_into_the_names_lookup() {
        ActiveRoleNamesLookupValidatorImpl validator = new ActiveRoleNamesLookupValidatorImpl(
                request -> {
                    assertThat(request).isEqualTo(REQUEST);
                    return Mono.just(new ActiveRolesResponse(USER_ID, APPLICATION, Set.of(ROLE_ID)));
                },
                roleIds -> {
                    assertThat(roleIds).containsExactly(ROLE_ID);
                    return Mono.just(Set.of("Coordinador académico"));
                });

        StepVerifier.create(validator.execute(REQUEST))
                .assertNext(names -> assertThat(names).containsExactly("Coordinador académico"))
                .verifyComplete();
    }

    @Test
    void no_active_roles_resolve_to_no_names_without_reaching_the_names_lookup() {
        ActiveRoleNamesLookupValidatorImpl validator = new ActiveRoleNamesLookupValidatorImpl(
                request -> Mono.just(new ActiveRolesResponse(USER_ID, APPLICATION, Set.of())),
                roleIds -> {
                    assertThat(roleIds).isEmpty();
                    return Mono.just(Set.of());
                });

        StepVerifier.create(validator.execute(REQUEST))
                .assertNext(names -> assertThat(names).isEmpty())
                .verifyComplete();
    }
}
