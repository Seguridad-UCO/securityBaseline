# Obligaciones — modelo único

Una obligación es algo que el **PEP tiene que hacer** antes de servir un `ALLOW`. No es un aviso: si
no se puede cumplir, el acceso no se sirve.

Schema: [`pdp-opa/v1/obligation.schema.json`](pdp-opa/v1/obligation.schema.json). La decisión que fijó
este modelo es la D-U3 de [`README.md`](README.md).

```json
{ "type": "AUDIT", "parameters": {} }
```

| Tipo | Qué exige del PEP |
|---|---|
| `AUDIT` | Registrar el acceso en la traza de auditoría antes de reenviar |
| `LOG_SECURITY_EVENT` | Emitir un evento de seguridad |
| `MASK_FIELDS` | Enmascarar los campos que indiquen los parámetros en la respuesta |
| `REQUIRE_MFA` | Exigir un segundo factor antes de reenviar |
| `READ_ONLY` | Reenviar solo si la operación no muta estado |

## Las dos reglas

1. **Tipo desconocido junto a un `ALLOW` ⇒ cerrar la solicitud.** Un PEP que no entiende una
   obligación no puede cumplirla, y servir el acceso sin cumplirla es peor que negarlo. Vale para
   cualquier consumidor, no solo para el PEP.
2. **Lista vacía es válida y frecuente.** La mayoría de los `ALLOW` no llevan obligaciones. Vacío
   significa «nada extra que hacer», no «algo faltó».

## Regla de transición mientras el PEP esté en v1

El PEP v1 declara `obligations` como array de **strings** y falla cerrado ante cualquier lista no
vacía. OPA ya emite objetos, y adjunta `AUDIT` en un `ALLOW` cross-tenant. Hasta que el PEP publique
su v1.1:

> **El PDP no descarta obligaciones para que el `ALLOW` pase.** Si la decisión trae una obligación y
> el canal de salida no puede transportarla, la respuesta es **`INDETERMINATE` /
> `CONTEXT_UNAVAILABLE`**, no un `ALLOW` recortado.

El razonamiento, porque es la parte que se presta a «arreglarlo» mal: filtrar la obligación hace que
el PEP acepte el `ALLOW` y sirva la petición **sin auditarla**. La política concedió el acceso *a
condición de* auditarlo; sin la condición, no concedió ese acceso. Degradar a `INDETERMINATE` falla
cerrado y deja rastro; filtrar falla abierto y no deja ninguno.

**Esta regla se retira** cuando `pep-pdp/v1.1` exista y el PEP la implemente. Es deuda con fecha de
caducidad, no un diseño.
