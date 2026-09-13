package co.edu.uco.seguridad.pdp.resources.application.rule.validator.impl;

import co.edu.uco.seguridad.pdp.commons.model.ResourceId;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ProtectedResourceLookup;
import co.edu.uco.seguridad.pdp.resources.application.rule.validator.ProtectedResourceIdLookupValidator;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import reactor.core.publisher.Mono;

public final class ProtectedResourceIdLookupValidatorImpl implements ProtectedResourceIdLookupValidator {
    private final ProtectedResourceRepository repository;
    public ProtectedResourceIdLookupValidatorImpl(ProtectedResourceRepository repository) { this.repository=repository; }
    @Override public Mono<ResourceId> execute(ProtectedResourceLookup lookup) { return repository.findIdByApplicationPathAndMethod(lookup.applicationId(), lookup.path(), lookup.method()); }
}
