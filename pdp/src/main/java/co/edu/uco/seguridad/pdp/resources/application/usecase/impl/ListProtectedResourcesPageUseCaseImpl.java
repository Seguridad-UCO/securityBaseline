package co.edu.uco.seguridad.pdp.resources.application.usecase.impl;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ListProtectedResourcesPageRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.secondaryport.repository.ProtectedResourceRepository;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesPageUseCase;
import reactor.core.publisher.Mono;
public final class ListProtectedResourcesPageUseCaseImpl implements ListProtectedResourcesPageUseCase {
 private final ProtectedResourceRepository repository; public ListProtectedResourcesPageUseCaseImpl(ProtectedResourceRepository repository){this.repository=repository;}
 public Mono<ResultPage<RegisteredProtectedResourceResponse>> execute(ListProtectedResourcesPageRequest input){return repository.findPageByApplication(input.applicationId(),input.window()).map(p->p.map(r->new RegisteredProtectedResourceResponse(r.id(),r.applicationId(),r.tenantId(),r.path(),r.method(),r.registeredAt())));}
}
