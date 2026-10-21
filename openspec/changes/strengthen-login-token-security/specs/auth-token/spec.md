# Spec Delta

## Purpose

Define los claims, la expiración y las protecciones observables de los tokens devueltos por el endpoint de login, preservando el contrato documentado salvo los cambios explícitos del Issue #12.

## ADDED Requirements

### Requirement: El token de login identifica al sujeto y al cliente
Con credenciales y un cliente válidos, el token devuelto SHALL exponer los claims verificables `sub`, `client`, `iat` y `exp`. `sub` SHALL identificar el username autenticado y `client` SHALL reflejar el valor recibido en el request.

#### Scenario: Login exitoso devuelve los claims requeridos
- **WHEN** un cliente envía username, password y client no vacío válidos a `POST /auth/login`
- **THEN** la respuesta es `200` y el token contiene `sub`, `client`, `iat` y `exp`
- **AND** `sub` identifica al usuario autenticado y `client` refleja el client enviado

### Requirement: La expiración del token coincide con la respuesta
El token SHALL vencer 3600 segundos después de su emisión y `expiresIn` SHALL representar la diferencia entre `exp` y el momento de la respuesta, con una tolerancia máxima de 1 segundo.

#### Scenario: Login exitoso informa una expiración coherente
- **WHEN** un cliente envía credenciales válidas y un client no vacío
- **THEN** `exp` identifica el vencimiento del token 3600 segundos después de su emisión
- **AND** la diferencia entre `expiresIn` y `exp - now` no supera 1 segundo

#### Scenario: Un token con expiración pasada se considera vencido
- **WHEN** se evalúa la expiración de un token cuyo `exp` es anterior al momento actual
- **THEN** el token se considera vencido
- **AND** este requisito no introduce un endpoint de validación ni un flujo de login adicional

### Requirement: El login valida el valor de client
`POST /auth/login` SHALL requerir un valor de client que contenga al menos un carácter distinto de espacio en blanco. Un client ausente o en blanco SHALL rechazarse con `400 Bad Request`.

#### Scenario: Client ausente
- **WHEN** un cliente envía username y password válidos sin `client`
- **THEN** la solicitud de login se rechaza con `400 Bad Request`

#### Scenario: Client vacío o compuesto por espacios
- **WHEN** un cliente envía un valor `client` vacío o compuesto solo por espacios en blanco
- **THEN** la solicitud de login se rechaza con `400 Bad Request`

#### Scenario: Client con caracteres no blancos
- **WHEN** un cliente envía un valor `client` con al menos un carácter distinto de espacio en blanco y credenciales válidas
- **THEN** el valor pasa la validación y se refleja en el claim `client` del token

### Requirement: Se preserva el contrato de login salvo los cambios indicados
El endpoint SHALL preservar el método, la ruta, la estructura JSON de respuesta, los campos del usuario, la marca debug, la validación de credenciales y el estado de credenciales inválidas documentados. Los cambios a la validación de `client` y a la expiración del token se limitan a lo especificado en este cambio.

#### Scenario: Credenciales válidas conservan la forma documentada de respuesta
- **WHEN** un cliente envía credenciales y client válidos a `POST /auth/login`
- **THEN** la respuesta es `200` y contiene `token`, `expiresIn`, `user` y la marca `debug` existente
- **AND** `user` contiene `username`, `displayName` y `role`

#### Scenario: Credenciales inválidas conservan el estado documentado
- **WHEN** un cliente envía un username desconocido o una password que no coincide
- **THEN** la respuesta es `401 Unauthorized`

### Requirement: El secreto del token no se usa implícitamente fuera del laboratorio
El servicio SHALL NOT usar un secreto implícito o hardcodeado fuera del laboratorio. Todo valor predeterminado que se conserve para laboratorio SHALL identificarse claramente como exclusivo de ese uso en la documentación de configuración.

#### Scenario: Configuración fuera del laboratorio no tiene secreto implícito
- **WHEN** el servicio se ejecuta fuera del laboratorio sin `AUTH_TOKEN_SECRET` configurado explícitamente
- **THEN** no emite tokens usando un secreto predeterminado o hardcodeado

#### Scenario: Un valor predeterminado de laboratorio queda documentado
- **WHEN** la documentación de configuración describe un secreto predeterminado retenido
- **THEN** indica claramente que es exclusivo del laboratorio y no debe usarse en otros entornos

### Requirement: Los logs de autenticación excluyen valores sensibles
El procesamiento de autenticación SHALL NOT registrar tokens, secretos de token, contraseñas ni otros valores sensibles de autenticación.

#### Scenario: Un login exitoso no registra valores sensibles
- **WHEN** un cliente completa un login exitoso
- **THEN** los logs de la aplicación no contienen el token emitido, el secreto configurado, la contraseña ni otros valores sensibles de autenticación

#### Scenario: Un login fallido no registra valores sensibles
- **WHEN** un cliente envía credenciales inválidas o un client inválido
- **THEN** los logs de la aplicación no contienen la contraseña enviada, el secreto configurado ni otros valores sensibles de autenticación

### Requirement: Se documenta el enfoque de firma del token
Antes de completar la implementación, la documentación del proyecto SHALL registrar si la firma del token usa HMAC o una librería JWT y explicar la alternativa elegida. Este requisito no prescribe ninguna de las dos alternativas.

#### Scenario: Se documentan el enfoque de firma y su justificación
- **WHEN** se revisa la documentación de firma de tokens
- **THEN** identifica el enfoque seleccionado como HMAC o librería JWT y registra su justificación
