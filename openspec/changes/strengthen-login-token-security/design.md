# Design

## Context

See [proposal.md](./proposal.md) for motivation and [spec.md](./specs/auth-token/spec.md) for the observable requirements.

The login endpoint is implemented by `AuthResource`; `LoginRequest` already maps a `client` field, but the endpoint does not currently validate or use it. `LegacyTokenUtil` produces Base64-encoded concatenated fields and embeds a secret; it does not currently create an HMAC signature. The project uses Java 21 and Quarkus 3.27.4 and has no JWT dependency. The existing `AuthResourceTest` covers only a basic successful login.

## Goals / Non-Goals

**Goals:**
- Define a verifiable token representation and an HMAC signing input that consumers can reproduce.
- Keep token signing isolated from HTTP response construction and make the signing key explicit configuration, with no implicit production default.
- Preserve the login response and authentication behavior except for the changes specified in the proposal and spec.
- Establish characterization tests for the existing contract before changing security-sensitive behavior.

**Non-Goals:**
- Add token verification, refresh, revocation, or authorization endpoints.
- Change user storage, credential verification, or unrelated endpoints.
- Introduce JWT compatibility or redesign the application.

## Contratos protegidos

Preserve `POST /auth/login`, the successful response fields (`token`, `expiresIn`, `user`, and existing `debug` marker), the documented user fields, and `401` for invalid credentials. Preserve the existing `400` behavior for a null request or invalid username/password. The only intended contract changes are requiring a non-blank `client` and changing token content and expiration semantics as specified in the spec.

## Componentes afectados

- `AuthResource`: validate `client`, request token creation after successful authentication, and return an expiration value consistent with the issued token.
- `LoginRequest`: retain the existing `client` request mapping.
- `LegacyTokenUtil`: replace the legacy encoding and embedded secret with the chosen HMAC representation and configured key.
- Configuration and endpoint documentation: describe the required secret configuration and token representation without documenting or exposing a real secret.
- `AuthResourceTest`: characterize the current contract and verify the new behavior.

## Decisions

1. **Use HMAC-SHA-256 through Java's built-in cryptography APIs; do not add a JWT dependency.** This follows the selected HMAC approach and avoids a new runtime dependency. A maintained JWT library was considered; it provides standard JWT parsing and interoperability but adds a dependency and an explicit JWT format. This design does not claim JWT compatibility.

2. **Use an explicitly defined signed token representation.** Serialize the claims as UTF-8 JSON with integer epoch-second `iat` and `exp` values. Base64URL-encode the payload without padding, sign the ASCII bytes of that encoded payload using HMAC-SHA-256, and Base64URL-encode the signature without padding. Emit the token as `<encoded-payload>.<encoded-signature>`. The encoded payload is the complete signing input, avoiding a dependency on JSON canonicalization during signature verification. Document the algorithm, encoding, and signing input for consumers.

3. **Derive expiration from one issue-time value.** Capture `iat` once, calculate `exp` as `iat + 3600`, and derive `expiresIn` from that `exp` and the time used to construct the response. Keep epoch-second precision and verify the spec's one-second tolerance in tests rather than independently setting a fixed response duration.

4. **Require explicit key configuration.** Supply the HMAC key through `AUTH_TOKEN_SECRET` configuration and do not embed or provide an implicit production fallback. If the key is missing, token issuance must fail closed; tests may provide a test-only value through test configuration. Never place a real key in source, documentation examples, or logs. A laboratory-only default was considered but rejected to avoid accidental use outside the lab.

5. **Validate `client` without normalizing it.** Reject null, empty, or whitespace-only values and place the original accepted value in the claim. Trimming or otherwise rewriting it was considered but would make the claim differ from the request value.

6. **Keep authentication logs free of sensitive values.** Do not add logging of the token, signing key, password, or other authentication secrets. Characterization and security tests should verify both success and failure paths without asserting or recording sensitive values.

## Pruebas de caracterización

Before changing the critical behavior, extend integration coverage to record:
- The successful response shape, existing `debug` marker, user fields, and current status.
- Existing `400` behavior for a null request and missing or blank username/password.
- Existing `401` behavior for invalid credentials.
- The current acceptance of an omitted `client` and fixed `expiresIn`, marking these as behavior intentionally changed by this proposal.
- The token's current shape only as a transient baseline; do not place the embedded secret or a complete secret-bearing token in test fixtures.

After the baseline is captured, update the affected assertions and verify required claims, HMAC signature using a test key, rejection of missing/blank `client`, unchanged response fields, expiration consistency within one second, missing-key fail-closed behavior, and absence of sensitive authentication values in logs. Characterization tests must not make the legacy token format or optional `client` permanent contracts.

## Estrategia incremental

1. Add and run characterization coverage against the current endpoint before changing behavior.
2. Require and validate `client`; test absent and blank values before integrating the issuer.
3. Add explicit key configuration and the HMAC token representation together with its HTTP call site; test the signing input and signature independently. Do not retain a legacy issuer as a compatibility bridge.
4. Integrate issue-time and expiration calculation with the response, verify the one-second requirement, and confirm that the accepted `client` value is preserved.
5. Verify protected response behavior, sensitive-log exclusions, configuration documentation, and the full relevant test suite.

## Riesgos / Trade-offs

- **A custom HMAC representation is less interoperable than JWT.** → Document the exact payload encoding, signing input, and algorithm; reassess a maintained JWT library if standard JWT interoperability becomes a requirement.
- **A weak, leaked, or inconsistent key undermines token integrity.** → Require explicit secret configuration, keep key material outside source and logs, and use the same configured key for issuers and any future verifier.
- **Consumers may depend on the legacy token format.** → Coordinate the format transition; do not assume old and new tokens are compatible or restore the embedded-secret format as a rollback.
- **Clock precision or response delay can make duration values diverge.** → Derive both claims from one issue-time value and test the stated tolerance against the actual response.
- **A missing secret can prevent issuance.** → Fail closed, report configuration failure without exposing the key, and configure a test-only key for automated tests.

## Migration Plan

1. Capture the current endpoint contract in tests.
2. Deploy the configured HMAC issuer together with the documented token format and update any consumers that parse legacy tokens.
3. Verify successful and failed login behavior, token claims/signature/expiry, and that no legacy embedded secret remains in the issuer.

No data migration is required because token issuance is stateless. If rollback is necessary, account for consumers already expecting the new token format. Roll back only to a version that does not reintroduce the embedded-secret token outside the laboratory; if a safe compatible version is unavailable, disable issuance until the configuration or compatibility issue is corrected.
