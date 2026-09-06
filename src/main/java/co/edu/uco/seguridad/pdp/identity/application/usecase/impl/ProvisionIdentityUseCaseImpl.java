package co.edu.uco.seguridad.pdp.identity.application.usecase.impl;

import co.edu.uco.seguridad.pdp.commons.model.TenantId;
import co.edu.uco.seguridad.pdp.identity.application.primaryport.request.ProvisionIdentityRequest;
import co.edu.uco.seguridad.pdp.identity.application.secondaryport.repository.SecurityUserRepository;
import co.edu.uco.seguridad.pdp.identity.application.usecase.ProvisionIdentityUseCase;
import co.edu.uco.seguridad.pdp.identity.domain.model.ExternalIdentity;
import co.edu.uco.seguridad.pdp.identity.domain.SecurityUser;
import co.edu.uco.seguridad.pdp.identity.domain.model.UserId;
import co.edu.uco.seguridad.shared.message.RequiredArgumentMessages;
import co.edu.uco.seguridad.shared.port.IdentifierGenerator;
import co.edu.uco.seguridad.shared.port.TimeProvider;
import co.edu.uco.seguridad.shared.security.LocalUserPrincipal;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Encuentra al usuario por identidad externa primero (issuer+subject); si no existe, por correo
 * (permite que Google y el login local de Keycloak converjan en el mismo usuario); si tampoco,
 * crea uno nuevo en el tenant por defecto y lo vincula.
 */
public final class ProvisionIdentityUseCaseImpl implements ProvisionIdentityUseCase {

    private final SecurityUserRepository repository;
    private final TenantId defaultTenantId;
    private final IdentifierGenerator identifiers;
    private final TimeProvider time;

    public ProvisionIdentityUseCaseImpl(SecurityUserRepository repository, TenantId defaultTenantId,
            IdentifierGenerator identifiers, TimeProvider time) {
        this.repository = Objects.requireNonNull(repository);
        this.defaultTenantId = Objects.requireNonNull(defaultTenantId, RequiredArgumentMessages.TENANT_ID);
        this.identifiers = Objects.requireNonNull(identifiers, RequiredArgumentMessages.IDENTIFIER_GENERATOR);
        this.time = Objects.requireNonNull(time, RequiredArgumentMessages.TIME_PROVIDER);
    }

    @Override
    public Mono<LocalUserPrincipal> execute(ProvisionIdentityRequest dto) {
        return repository.findIdentity(dto.issuer(), dto.subject())
                .flatMap(identity -> loginKnownUser(identity.userId(), dto))
                .switchIfEmpty(Mono.defer(() -> repository.findByEmail(dto.email())
                        .flatMap(existing -> linkAndLogin(existing, dto))
                        .switchIfEmpty(Mono.defer(() -> createAndLink(dto)))));
    }

    private Mono<LocalUserPrincipal> loginKnownUser(UserId userId, ProvisionIdentityRequest dto) {
        return repository.findById(userId)
                .map(user -> user.withLogin(dto.name(), time.now()))
                .flatMap(repository::save)
                .map(user -> toPrincipal(user, dto.subject()));
    }

    private Mono<LocalUserPrincipal> linkAndLogin(SecurityUser existing, ProvisionIdentityRequest dto) {
        ExternalIdentity identity = new ExternalIdentity(existing.id(), dto.issuer(), dto.subject(), dto.provider());
        return repository.linkIdentity(identity)
                .then(repository.save(existing.withLogin(dto.name(), time.now())))
                .map(user -> toPrincipal(user, dto.subject()));
    }

    private Mono<LocalUserPrincipal> createAndLink(ProvisionIdentityRequest dto) {
        SecurityUser user = SecurityUser.provision(new UserId(identifiers.next()),
                defaultTenantId, dto.email(), dto.name(), time.now());
        ExternalIdentity identity = new ExternalIdentity(user.id(), dto.issuer(), dto.subject(), dto.provider());
        return repository.save(user)
                .flatMap(saved -> repository.linkIdentity(identity).thenReturn(saved))
                .map(saved -> toPrincipal(saved, dto.subject()));
    }

    private static LocalUserPrincipal toPrincipal(SecurityUser user, String subject) {
        return new LocalUserPrincipal(user.id().value().toString(), subject, user.tenantId(), user.email().value(),
                user.name());
    }
}
