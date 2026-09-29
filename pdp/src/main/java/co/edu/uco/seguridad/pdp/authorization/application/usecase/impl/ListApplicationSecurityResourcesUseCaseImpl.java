package co.edu.uco.seguridad.pdp.authorization.application.usecase.impl;
import co.edu.uco.seguridad.pdp.authorization.application.primaryport.request.ListApplicationSecurityResourcesRequest;
import co.edu.uco.seguridad.pdp.authorization.application.rule.validator.PrincipalMustBeApplicationAdministratorValidator;
import co.edu.uco.seguridad.pdp.authorization.application.usecase.ListApplicationSecurityResourcesUseCase;
import co.edu.uco.seguridad.pdp.commons.model.ResultPage;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.request.ListProtectedResourcesPageRequest;
import co.edu.uco.seguridad.pdp.resources.application.primaryport.response.RegisteredProtectedResourceResponse;
import co.edu.uco.seguridad.pdp.resources.application.usecase.ListProtectedResourcesPageUseCase;
import reactor.core.publisher.Mono;
public final class ListApplicationSecurityResourcesUseCaseImpl implements ListApplicationSecurityResourcesUseCase {
 private final PrincipalMustBeApplicationAdministratorValidator administrator; private final ListProtectedResourcesPageUseCase resources;
 public ListApplicationSecurityResourcesUseCaseImpl(PrincipalMustBeApplicationAdministratorValidator administrator,ListProtectedResourcesPageUseCase resources){this.administrator=administrator;this.resources=resources;}
 public Mono<ResultPage<RegisteredProtectedResourceResponse>> execute(ListApplicationSecurityResourcesRequest input){return administrator.execute(input.administration()).then(Mono.defer(()->resources.execute(new ListProtectedResourcesPageRequest(input.administration().applicationId(),input.window()))));}
}
