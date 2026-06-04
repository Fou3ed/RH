# Security Posture — MARAM Payroll Platform

This document records the platform's security controls and its posture against the
OWASP Top 10 (2021). It is maintained alongside the code and reviewed each sprint.

## Authentication & session management
- **JWT, stateless** — short-lived HS256 access tokens (15 min) carrying roles +
  permissions; opaque refresh tokens (7 days) stored server-side and **rotated** on
  every refresh, so they can be revoked at logout (`refresh_tokens` table).
- **Password storage** — BCrypt (`BCryptPasswordEncoder`), per-password salt.
- **Brute-force lockout** — accounts are **LOCKED after 5 failed attempts**
  (`LoginAttemptService`, committed independently of the failed login transaction).
  A locked account is rejected even with the correct password.
- Failure responses are generic (`Invalid credentials`) — no user-enumeration signal.

## Authorisation (RBAC)
- 5 roles, 20 permissions, enforced with method security (`@PreAuthorize`) on every
  business endpoint. Authorities = role codes (`ROLE_*`) + fine-grained permissions.
- JSON `401` (unauthenticated) and `403` (insufficient permission) envelopes.

## Transport & headers
- CORS restricted to the known frontend origins, credentials allowed.
- Security response headers: `X-Content-Type-Options: nosniff`,
  `X-Frame-Options: DENY`, `Content-Security-Policy` (`frame-ancestors 'none'`,
  `object-src 'none'`), `Referrer-Policy: strict-origin-when-cross-origin`,
  and HSTS (`max-age=31536000; includeSubDomains`, effective over HTTPS).
- Production profile disables error messages/stack traces in responses.

## OWASP Top 10 (2021) mapping

| # | Risk | Mitigation |
|---|------|-----------|
| A01 | Broken Access Control | RBAC via `@PreAuthorize` on all endpoints; stateless JWT; no IDOR-by-default (lookups are owner-scoped where applicable). |
| A02 | Cryptographic Failures | BCrypt password hashing; HS256-signed JWTs; secrets via env vars (never committed). HSTS in prod. |
| A03 | Injection | 100% parameterised access via Spring Data JPA / bound query params — verified by `SecurityHardeningIntegrationTest` (a `DROP TABLE` payload is treated as literal text). No string-concatenated SQL. Bean Validation on all request DTOs. |
| A04 | Insecure Design | Layered architecture, immutable audit trail (`audit_log`), payroll period lifecycle state machine, all-or-nothing imports. |
| A05 | Security Misconfiguration | Security headers set explicitly; CSRF disabled only because the API is stateless+token-based; actuator exposes only `health`/`info`/`metrics`; prod profile hides errors. |
| A06 | Vulnerable Components | Pinned dependency versions; Spring Boot BOM manages transitive versions. (Recommend Dependabot/`mvn versions:display-dependency-updates` in CI.) |
| A07 | Identification & Auth Failures | Account lockout, BCrypt, generic auth errors, refresh-token rotation + revocation. |
| A08 | Software & Data Integrity | Flyway-owned schema (`ddl-auto: validate`); audited mutations; CI builds from source. |
| A09 | Logging & Monitoring | Business actions written to `audit_log` (actor, entity, action, timestamp); actuator health probes. (Recommend shipping logs to a SIEM in prod.) |
| A10 | SSRF | No server-side fetching of user-supplied URLs. MinIO/Redis endpoints are configuration, not request-derived. |

## Verified by automated tests
- `SecurityHardeningIntegrationTest` — brute-force lockout, SQL-injection-safe
  search, presence of security headers.
- `AuthIntegrationTest` — unauthenticated `401`, invalid credentials `401`.
- Authorization is exercised across the employee/attendance/payroll/config suites
  (every endpoint is `@PreAuthorize`-guarded).

## Known gaps / recommendations (not yet implemented)
- **Rate limiting** at the gateway/ingress (lockout covers credential stuffing per
  account, not volumetric abuse).
- **Dependency scanning** in CI (Dependabot / OWASP Dependency-Check).
- **Secrets management** — move from `.env` to a vault for production.
- **TLS termination** — enforce HTTPS at the ingress; HSTS only takes effect there.
- **Email/MFA** — multi-factor authentication is not implemented.

## Reporting a vulnerability
Email the engineering team; do not open a public issue for security reports.
