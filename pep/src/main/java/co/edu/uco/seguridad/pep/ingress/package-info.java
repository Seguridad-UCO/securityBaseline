@ApplicationModule(allowedDependencies = {
        "application", "commons",
        "enforcement", "enforcement :: usecase", "enforcement :: request",
        "normalization :: usecase", "normalization :: request"})
package co.edu.uco.seguridad.pep.ingress;

import org.springframework.modulith.ApplicationModule;
