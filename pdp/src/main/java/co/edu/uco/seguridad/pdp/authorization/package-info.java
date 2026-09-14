@org.springframework.modulith.ApplicationModule(allowedDependencies = {
        "commons",
        "applications", "applications :: rule", "applications :: dto", "applications :: exception",
        "resources", "resources :: rule", "resources :: dto", "resources :: model", "resources :: exception",
        "assignments :: usecase", "assignments :: dto",
        "identity :: usecase",
        "roles :: rule"})
package co.edu.uco.seguridad.pdp.authorization;
