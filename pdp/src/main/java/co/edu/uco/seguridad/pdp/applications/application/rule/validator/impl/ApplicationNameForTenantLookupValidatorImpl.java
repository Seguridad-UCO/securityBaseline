package co.edu.uco.seguridad.pdp.applications.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.applications.application.primaryport.request.ApplicationNameForTenantQuery;
import co.edu.uco.seguridad.pdp.applications.application.primaryport.response.RegisteredApplicationResponse;
import co.edu.uco.seguridad.pdp.applications.application.rule.validator.ApplicationNameForTenantLookupValidator;
import co.edu.uco.seguridad.pdp.applications.application.secondaryport.repository.ApplicationRepository;
import reactor.core.publisher.Mono;
import java.util.Objects;
public final class ApplicationNameForTenantLookupValidatorImpl implements ApplicationNameForTenantLookupValidator {
 private final ApplicationRepository repository; public ApplicationNameForTenantLookupValidatorImpl(ApplicationRepository repository){this.repository=Objects.requireNonNull(repository);}
 public Mono<RegisteredApplicationResponse> execute(ApplicationNameForTenantQuery query){return repository.findByTenantAndName(query.tenantId(),query.name()).map(a->new RegisteredApplicationResponse(a.id(),a.tenantId(),a.name(),a.description(),a.baseUrl(),a.registeredAt(),a.enabledEnvironments()));}
}
