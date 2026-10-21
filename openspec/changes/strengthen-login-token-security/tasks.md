# Tasks

Orden de ejecucion aprobado: 1.1, 1.2, 4.1, 2.1, 2.2, 3.1, 4.2, 2.3, 5.1 y 5.2. La validacion de client precede a la integracion del emisor. Las tareas 2.1 y 2.2 forman un incremento compatible de emisor y consumidor; no se conserva un emisor legacy como puente.

## 1. Pruebas de caracterización previas al cambio

- [x] 1.1 En `src/test/java/com/bcp/workshop/auth/AuthResourceTest.java`, ampliar la prueba de login exitoso para caracterizar antes de cambiar el comportamiento el estado 200, los campos `token`, `expiresIn`, `user` y `debug`, los campos de usuario y el valor actual `expiresIn = 3600`; registrar también que actualmente se acepta `client` ausente sin guardar el token completo ni el secreto. Verificar con `mvn test -Dtest=AuthResourceTest`.
- [x] 1.2 En `AuthResourceTest.java`, caracterizar los estados actuales para request nulo, username/password ausentes o en blanco y credenciales inválidas; verificar que el resultado conserva los estados 400/401 documentados. Verificar con `mvn test -Dtest=AuthResourceTest`.

## 2. Emisión HMAC y configuración explícita

- [x] 2.1 Reemplazar en `src/main/java/com/bcp/workshop/auth/LegacyTokenUtil.java` la codificación legacy y el secreto embebido por el formato HMAC-SHA-256 definido en `design.md`, con claims `sub`, `client`, `iat` y `exp`; añadir pruebas unitarias para claims y firma reproducible usando una clave de prueba. Verificar con `mvn test -Dtest=LegacyTokenUtilTest`.
- [x] 2.2 Integrar el emisor desde `src/main/java/com/bcp/workshop/auth/AuthResource.java`, obtener la clave desde `AUTH_TOKEN_SECRET` sin fallback productivo, pasar el `client` validado y fallar de forma segura si no hay clave; configurar una clave solo de pruebas y probar la respuesta y el caso sin clave. Verificar con `mvn test -Dtest=AuthResourceTest`.
- [x] 2.3 Actualizar `docs/ENDPOINTS.md` para describir el formato firmado elegido, sus claims y la configuración requerida de `AUTH_TOKEN_SECRET`, incluyendo que no hay un secreto productivo implícito; no incluir una clave real ni el token legacy completo. Verificar que la documentación coincide con el formato implementado y ejecutar `mvn test -Dtest=AuthResourceTest`.

## 3. Expiración coherente con la respuesta

- [x] 3.1 Actualizar la creación del token y `expiresIn` en `src/main/java/com/bcp/workshop/auth/AuthResource.java` para derivar `exp` de `iat + 3600` y calcular `expiresIn` respecto del momento de respuesta; añadir una prueba que compruebe `exp - iat = 3600` y una diferencia máxima de un segundo entre `expiresIn` y `exp - now`. Verificar con `mvn test -Dtest=AuthResourceTest`.

## 4. Validación de client

- [x] 4.1 Validar en `AuthResource.java` que `client` esté presente y no sea vacío ni compuesto solo por espacios, respondiendo 400 en esos casos; añadir casos de integración para client ausente, vacío y en blanco, y actualizar en `AuthResourceTest.java` la caracterización transitoria de client opcional. Verificar con `mvn test -Dtest=AuthResourceTest`.
- [x] 4.2 Añadir un caso de integración para login válido que compruebe que el claim `client` refleja exactamente el valor no blanco recibido, sin normalizarlo, y que el resto de la respuesta exitosa conserva su forma documentada. Verificar con `mvn test -Dtest=AuthResourceTest`.

## 5. Seguridad y regresión del contrato

- [x] 5.1 Añadir verificación de logs para login exitoso y fallido que compruebe que no aparecen el token, la clave HMAC, la contraseña ni otros valores sensibles; ajustar únicamente el manejo de logs del flujo de login si la prueba detecta exposición. Verificar con `mvn test -Dtest=AuthResourceTest`.
- [x] 5.2 Ejecutar la suite completa y validar el cambio OpenSpec, comprobando que los casos anteriores pasan y que no se reintrodujo el secreto embebido en el emisor. Verificar con `mvn test` y `openspec validate strengthen-login-token-security --json`.

## Evidencia local (2026-10-21)

Issue de producto: [#12](https://github.com/user-bcp-student23/workshop-auth-backend/issues/12). Rama conservada: `feature/HU-01`. Todos los comandos siguientes se ejecutaron desde `workshop-auth-backend`.

| Incremento | Comando | Resultado |
|---|---|---|
| Orden del plan | `openspec validate strengthen-login-token-security --json` | PASS, sin incidencias |
| 4.1 | `mvn -q -Dtest=AuthResourceTest -Dquarkus.http.test-port=0 test` | PASS, 6 tests |
| 2.1 / 2.2 | `mvn -q -Dtest=LegacyTokenUtilTest,AuthResourceTest test` | PASS, 10 tests |
| 3.1 / 4.2 | `mvn -q -Dtest=LegacyTokenUtilTest,AuthResourceTest test` | PASS, 12 tests tras corregir un matcher decimal aplicado a un entero |
| 2.3 / 5.1 | `mvn -q -Dtest=AuthResourceTest test` | PASS, 9 tests |
| Suite y cobertura | `mvn -q -Djacoco.destFile=target/jacoco-token.exec -Djacoco.dataFile=target/jacoco-token.exec verify` | PASS, 13 tests, 0 fallos/errores/omitidos |
| Cierre 5.2 | `mvn -q test` | PASS, 13 tests, 0 fallos/errores/omitidos |
| Whitespace | `git diff --check` | PASS |

Cobertura de lineas de las clases de produccion modificadas en `target/site/jacoco/jacoco.xml`: `AuthResource` 100% (29/29), `LegacyTokenUtil` 88.89% (16/18). Sin coincidencias de la clave legacy en el emisor. Los resultados de apply confirmaron el progreso tras cada grupo completado.

Protecciones verificadas: forma de respuesta y estados 400/401; client obligatorio y preservado sin normalizar; claims y firma reproducible; ausencia de clave impide emitir token; exp - iat = 3600 y expiresIn coherente; evaluacion controlada de exp pasado; logs de login exitoso/fallido sin token, clave ni contrasenas. No se agregan endpoints verificadores ni refresh.

Pendiente de otros gates: revision humana, CI y sus controles de seguridad; integracion con consumidores del nuevo formato y suministro seguro de la clave de despliegue. La prueba sin clave cubre el Resource con persistencia de prueba, no un despliegue externo. No se afirma validacion de esos entornos. El entorno local tiene JAVA_HOME de JDK 25; una ejecucion intermedia mostro avisos de instrumentacion de JaCoCo/clases del runtime. No se desactivo JaCoCo y el reporte final de clases de produccion se genero; conviene repetir en el JDK 21 requerido en CI.

No aplicable: migracion de datos, endpoints nuevos, cambios de persistencia, CORS o remediacion SCA. No se crearon commits ni se cambio de rama. El aviso del editor sobre `AUTH_TOKEN_SECRET` sin valor en configuracion principal es esperado: la clave es obligatoriamente externa y no tiene respaldo productivo.
