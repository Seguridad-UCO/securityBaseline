# Estado de HU-027 — validación local parcial (2026-10-28)

> **No cerrar la historia todavía.** Esta actualización sustituye las notas anteriores que decían
> que Docker no estaba disponible: el stack local sí se ejecutó y las suites indicadas abajo pasaron.
> La aceptación global sigue pendiente por criterios de CI, modo detection, observabilidad y medición.
> Ver el [handoff vigente](../HANDOFF-VALIDACION-HU-027-029.md).

## Trabajo presente en el árbol

- Configuración de Caddy + Coraza/CRS, overlays PDP/PEP y suites Hurl.
- Pipeline dedicado en `ci/edge-pipeline.yml`; no está registrado ni ejecutado en Azure DevOps.
- Receptor de logs y scrape Prometheus del edge; dashboard actualizado.
- `EDGE_MODE` por defecto conserva el comportamiento documentado en el plan; la prueba local de esta
  actualización inició Edge explícitamente en `blocking`.

## Comprobaciones locales ejecutadas

| Comprobación | Resultado |
|---|---|
| Flujos Hurl en `blocking`, secuencial (`--jobs 1`) | ✅ 8 archivos, 89 solicitudes |
| Ataques Hurl en `blocking` con `--path-as-is` | ✅ 8 archivos, 11 solicitudes |
| Rate limit: 61 solicitudes desde una IP y 50 desde otra, en paralelo | ✅ Ambos casos |
| Login OIDC BFF, sesión y logout a través de Edge/Keycloak | ✅ 9 solicitudes |
| `authorize.hurl` | ✅ Corregido `action: read` a verbo HTTP `GET` requerido por el PDP |
| Edge/PDP/Keycloak/Redis de Compose | ✅ Stack local healthy para la ejecución |

Las corridas autenticadas se ejecutaron secuencialmente para no activar el bloqueo anti-fuerza-bruta
de Keycloak. Una primera ejecución paralela sí bloqueó temporalmente al usuario local `edge-tester`;
se limpió el contador solo para esa cuenta de pruebas. Para los casos de path traversal se usó
`--path-as-is`, de modo que el cliente no normalizara la URL codificada antes de enviarla.

## Criterios aún no validados

1. Repetir los flujos en `detection` y verificar el audit log de Coraza para que esté vacío en las
   solicitudes legítimas; el health/audit log en ese modo había fallado previamente.
2. Ejecutar el overlay PEP y sus suites contra el stack real.
3. Registrar/ejecutar `ci/edge-pipeline.yml` en Azure DevOps, incluida la prueba fail-closed.
4. Verificar de extremo a extremo los campos de observabilidad exigidos (regla, score, ruta y
   correlación) y que la sanitización no filtre datos sensibles.
5. Medir p50/p95 con y sin edge; el spike de sidecar y memoria en Azure sigue pendiente.
6. Ejecutar `verificar.ps1`/`mvnw verify` completo en la rama final y clasificar los fallos de
   Modulith/ArchUnit/consistencia/drift.

**Estado final:** suites locales del PDP-edge en `blocking` aprobadas; HU-027 **parcialmente validada,
no aprobada en su totalidad**. No promover `blocking` a valor predeterminado ni afirmar validación de
Azure/CI sin evidencia correspondiente.
