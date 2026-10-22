# Informe frontend — ReqFlow

## Estado: BLOQUEADO; no se implementó el recorrido

Faltan el Issue en alcance, su bloque `ALCANCE`, número, hallazgo `H-xx` y criterios de aceptación. Tampoco se proporcionaron el repositorio frontend, las specs aplicables, `docs/ENDPOINTS.md`, `HALLAZGOS.md` ni la Definition of Done.

Sin esos artefactos no es posible confirmar qué recorrido está autorizado, definir sus pantallas y campos, ni implementar o verificar el contrato sin inventar requisitos. La mención de `POST /auth/login` en la constitución no demuestra que el Issue corresponda al login ni describe el contrato completo.

## Criterios del Issue cubiertos

No identificables: no se proporcionaron el Issue ni sus criterios. No se afirma que algún criterio esté cubierto.

## Contrato y contraste con el backend

El material recibido confirma que la constitución enumera `POST /auth/login`. El fragmento de backend incluye un `LoginRequest` con `username`, `password` y `client`, todos `String`; una prueba envía esos campos y espera HTTP 200, `user.username` y un `token` no nulo.

Eso no basta para establecer el contrato aprobado: no confirma obligatoriedad, límites, validaciones, respuesta completa ni errores. Tampoco se aportaron la especificación del endpoint ni la implementación completa y activa para contrastarlos. No se añaden rutas ni se atribuye al Issue un flujo de login.

## Recorrido, campos, validaciones, acciones y mensajes

Pendientes hasta recibir el Issue, sus criterios y el contrato completo. No se definen etiquetas, valores iniciales, validaciones específicas ni mensajes porque no hay base aprobada para hacerlo.

Cuando se desbloquee, deberá verificarse, según el contrato aplicable:
- estados de carga, vacío, éxito y error;
- errores de campo, negocio y permisos, si corresponden;
- error de red y recuperación conservando los datos introducidos;
- bloqueo de envíos duplicados, progreso visible y liberación del bloqueo al finalizar;
- asociación accesible de etiquetas y errores, navegación por teclado, foco visible, anuncios de estado y gestión del foco al validar o cambiar de vista.

La validación del cliente no sustituirá la validación ni la autorización del servidor. No se implementa ni se propone un reintento automático de escrituras.

## Comprobación

**Pruebas propuestas; NO EJECUTADO.** No se ejecutaron herramientas ni se modificaron archivos.

No se proporcionaron el frontend ni sus archivos de configuración; por ello no es posible identificar las herramientas o comandos de prueba existentes y no se inventan comandos. Tampoco pueden darse datos de prueba aprobados ni resultados esperados específicos del contrato.

Para preparar una comprobación reproducible se necesitan el repositorio frontend, sus instrucciones de prueba y el contrato completo. Con esos insumos, la propuesta deberá documentar precondiciones, datos autorizados, pasos y resultados esperados para:
- caso válido y respuesta vacía, si aplica;
- campos inválidos y valores en los límites del contrato;
- errores de API, permisos y red, y recuperación;
- carga lenta y doble envío;
- uso por teclado y gestión del foco.

## Observaciones de la revisión anterior

- **Issue, número, `H-xx` y criterios ausentes:** sigue bloqueando la autorización y la trazabilidad. No hay corrección de código posible sin el alcance; la comprobación pendiente es recibir los artefactos aprobados.
- **CRÍTICO: R2 —** el fragmento de `LegacyTokenUtil` contiene el valor estático `WORKSHOP_SECRET_2026`. El hallazgo continúa pendiente. No se corrige porque el cambio solicitado es frontend y el flujo activo no está confirmado. La comprobación requiere revisar el backend completo y su configuración; no se ejecutó.
- **CRÍTICO: R4 —** el fragmento de `LegacyTokenUtil` codifica datos en Base64, que no constituye una firma. No se confirmó si participa en el flujo activo ni si se valida firma y expiración. El hallazgo continúa pendiente y no se modifica desde este alcance frontend. La comprobación requiere revisar el flujo activo y las pruebas correspondientes; no se ejecutó.
- **R6 —** no hay cambios, pruebas ejecutadas ni evidencia de cobertura. No puede verificarse caracterización previa ni cobertura mínima del 85 %. La comprobación queda pendiente de los artefactos del producto y las herramientas disponibles.
- Se conserva la limitación ya reportada: los informes previos no aportan resultados de herramientas ni evidencia ejecutada. Este informe tampoco presenta resultados no observados.

## Archivos frontend modificados

Ninguno. No se proporcionaron archivos frontend y no se realizaron cambios.