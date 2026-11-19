@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "tenants", "tenants :: dto", "tenants :: rule",
        "applications", "applications :: rule", "applications :: dto", "applications :: exception",
        "applications :: usecase", "applications :: model"})
package co.edu.uco.seguridad.pdp.resources;
