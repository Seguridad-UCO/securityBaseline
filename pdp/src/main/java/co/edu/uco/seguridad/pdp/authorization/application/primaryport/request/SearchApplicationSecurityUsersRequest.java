package co.edu.uco.seguridad.pdp.authorization.application.primaryport.request;
import co.edu.uco.seguridad.pdp.commons.model.PageWindow; import java.util.Objects;
public record SearchApplicationSecurityUsersRequest(AdministrationRequest administration,String query,PageWindow window){public SearchApplicationSecurityUsersRequest{Objects.requireNonNull(administration);query=query==null?"":query.trim();Objects.requireNonNull(window);}}
