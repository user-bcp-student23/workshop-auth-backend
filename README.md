# Workshop Auth Backend — Java Quarkus

> **Laboratorio intencionalmente vulnerable.** Ejecutar únicamente en local, repositorios privados y ambientes aislados. No desplegar en Internet, CERT o PROD.

Backend Quarkus que autentica usuarios de laboratorio almacenados en SQLite y entrega un token consumido por el frontend Angular y la API Python.

## Ejecución

Requisitos: JDK 21 y Maven 3.9+.

En esta rama, configurar `AUTH_TOKEN_SECRET` explicitamente en el entorno antes
de ejecutar Maven. No hay una clave predeterminada: sin clave no se emiten tokens.
No usar la clave exclusiva de pruebas para despliegues. El login requiere un
`client` no blanco. El formato HMAC no JWT y su transicion estan documentados en
[docs/ENDPOINTS.md](docs/ENDPOINTS.md).

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

## Propósito de la rama `main`

La rama contiene deuda técnica y de seguridad deliberada: SQL concatenado, contraseñas en texto plano, token reversible, secreto hardcodeado, CORS permisivo, método largo, manejo genérico de excepciones y dependencias con CVEs.
