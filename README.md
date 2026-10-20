# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21 y Maven 3.9+.

Configura `AUTH_TOKEN_SECRET` antes de iniciar este servicio y `workshop-requirements-api`. Ambos deben usar el mismo valor, con al menos 32 bytes UTF-8. No hay una clave predeterminada; usa un valor aleatorio y no lo guardes en el repositorio. Los tokens emitidos antes del cambio ya no son válidos: inicia sesión otra vez.

```bash
mvn quarkus:dev
```

Servicio: `http://localhost:8080`

Credenciales locales:

- `student` / `Workshop123!`
- `trainer` / `Trainer123!`

## Endpoints

- `POST /auth/login`
- `GET /q/health`

## Estado del laboratorio

El login usa SQL parametrizado y emite tokens HMAC firmados. Permanecen otras debilidades intencionales del laboratorio, como contraseñas en texto plano, CORS permisivo, métodos largos, manejo genérico de excepciones y dependencias con CVEs.
