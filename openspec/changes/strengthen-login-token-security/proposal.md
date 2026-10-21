# Proposal

## Why

El Issue [#12](https://github.com/user-bcp-student23/workshop-auth-backend/issues/12) compromete mejorar la seguridad y verificabilidad del token emitido por `POST /auth/login`: hoy el contrato no permite comprobar sus claims ni relacionar de forma fiable su expiración con `expiresIn`. Esto importa para que los consumidores puedan asociar un token con el cliente que lo solicitó y conocer cuándo vence, sin exponer secretos o credenciales.

## What Changes

- Emitir un token con los claims verificables `sub`, `client`, `iat` y `exp`, y hacer que `expiresIn` sea coherente con `exp - now`, con tolerancia máxima de 1 segundo.
- Usar y validar `client` desde el request. A diferencia del contrato del Día 1, donde `client` es opcional y no participa en la autenticación, ahora será obligatorio y un valor ausente, vacío o compuesto solo por espacios responderá `400`.
- Conservar el vencimiento nominal actual de 3600 segundos, el flujo de autenticación por credenciales y las respuestas documentadas para credenciales inválidas.
- No permitir que un secreto predeterminado se use fuera del laboratorio. Si se conserva un valor de laboratorio, su carácter exclusivo deberá quedar claramente documentado. No registrar tokens, secretos, credenciales ni otros valores sensibles.
- Registrar antes de la implementación la decisión técnica entre conservar HMAC o adoptar una librería JWT, sin seleccionar una alternativa en esta propuesta.

**Comportamiento que se preserva:** método y ruta `POST /auth/login`, request JSON, validación de `username` y `password`, respuesta `200` con `token`, `expiresIn` y `user`, sus campos documentados (`username`, `displayName`, `role`), la marca `debug` actual y la respuesta `401` para credenciales inválidas. El token seguirá siendo un string en la respuesta; cambia su contenido para cumplir los claims del Issue. `expiresIn` deja de ser un valor fijo informado independientemente del token y pasa a reflejar su expiración.

**Fuera de alcance:** agregar endpoints de validación o refresh de tokens; cambiar el flujo o los criterios de autenticación por usuario y contraseña; cambiar la forma pública de la respuesta, salvo la semántica de expiración indicada; decidir ahora entre HMAC y una librería JWT; y modificar el endpoint de salud.

## Capabilities

### New Capabilities

- `auth-token`: claims, expiración y protecciones observables del token emitido por el login.

### Modified Capabilities

- Ninguna. El repositorio no tiene specs existentes.

## Impact

- API: `POST /auth/login`; `client` pasa de opcional e ignorado a obligatorio y validado, y el contenido del token y la semántica de `expiresIn` cambian según el Issue.
- Configuración y documentación del secreto `AUTH_TOKEN_SECRET`, además de las pruebas y el manejo de logs relacionados con autenticación.
- Hay una discrepancia de contexto que debe tenerse presente: el Issue describe la implementación actual como HMAC con `AUTH_TOKEN_SECRET`, mientras que [docs/ENDPOINTS.md](../../../docs/ENDPOINTS.md) y el código actual describen un valor Base64 con campos concatenados y un secreto embebido. Esta propuesta toma el contrato del Día 1 como baseline de comportamiento y no presupone cuál mecanismo criptográfico se escogerá.
