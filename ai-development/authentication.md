# Feature: Authentication & Authorization

Users can register/log in, and the API distinguishes two roles: `BUYER` and `ADMIN`.
Auth is stateless JWT (short-lived access token + revocable refresh token). No admin
self-registration endpoint exists — the first admin is seeded manually, and admins
promote other users afterwards.

**Out of scope for v1:** forgot/reset password, email verification, login rate limiting.

---

## Data Model

### `User` (document, collection `users`)
- `id` — Mongo ObjectId
- `email` — unique, indexed, used as login identifier
- `passwordHash` — BCrypt hash, never returned in any response DTO
- `role` — enum `BUYER` | `ADMIN`
- `createdAt`

### `RefreshToken` (document, collection `refresh_tokens`)
- `id`
- `userId`
- `tokenHash` — store a hash of the token, not the raw value
- `expiresAt`
- `revoked` — boolean, set true on logout or token rotation

---

## Endpoints (`/api/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | public | Creates a new `BUYER` user (email + password). Always creates `BUYER` — no role field accepted from the client. |
| `POST` | `/api/auth/login` | public | Validates email/password, returns access token + refresh token. |
| `POST` | `/api/auth/refresh` | public (refresh token in body) | Validates refresh token, rotates it (revoke old, issue new), returns new access token. |
| `POST` | `/api/auth/logout` | authenticated | Revokes the caller's refresh token. |

## Admin creation (out-of-band, not an HTTP endpoint)
- First admin: seeded on startup via a `CommandLineRunner` reading `ADMIN_SEED_EMAIL` / `ADMIN_SEED_PASSWORD` env vars — only runs if no user with `ADMIN` role exists yet.
- Further admins: an authenticated `ADMIN` calls a role-management endpoint (e.g. `PATCH /api/users/{id}/role`) to promote another existing user. This endpoint itself must be `@PreAuthorize("hasRole('ADMIN')")`.

---

## Spring Security wiring

- Add `spring-boot-starter-security` + a JWT library (e.g. `jjwt`) to `pom.xml`.
- `SecurityFilterChain` bean: stateless session policy, CSRF disabled (pure REST API, no cookies), permit `/api/auth/**` and `GET` product/image read endpoints, everything else authenticated.
- Custom `JwtAuthenticationFilter` (`OncePerRequestFilter`): reads `Authorization: Bearer <token>`, validates signature/expiry, sets `SecurityContext` with the user's id + role.
- `@EnableMethodSecurity` so `@PreAuthorize("hasRole('ADMIN')")` works on controller methods.
- `PasswordEncoder` bean → `BCryptPasswordEncoder`.
- Custom `AuthenticationEntryPoint` / `AccessDeniedHandler` so 401/403 return JSON error bodies, not the default HTML error page.
- Access token claims: `sub` = userId, `role`, `exp` (short-lived, e.g. 15 min). Refresh token: opaque random value, hashed at rest, longer-lived (e.g. 30 days).

## Retrofit existing endpoints
- `ProductController` / `ImageController` write operations (create/update product, upload/delete/reorder images) → add `@PreAuthorize("hasRole('ADMIN')")`.
- Read/list endpoints (`GET /api/products`, `GET /api/images/{id}`) stay public.

---

## Checklist

- [ ] Add `spring-boot-starter-security` and JWT library dependency to `pom.xml`
- [ ] `User` document + `UserRepository`
- [ ] `RefreshToken` document + `RefreshTokenRepository`
- [ ] `PasswordEncoder` bean (BCrypt)
- [ ] JWT utility class: generate/parse/validate access tokens, generate/hash refresh tokens
- [ ] `JwtAuthenticationFilter`
- [ ] `SecurityFilterChain` config (public vs authenticated routes, stateless sessions, CSRF disabled)
- [ ] Custom 401/403 JSON error handlers
- [ ] `@EnableMethodSecurity` enabled
- [ ] `AuthController`: `POST /api/auth/register`
- [ ] `AuthController`: `POST /api/auth/login`
- [ ] `AuthController`: `POST /api/auth/refresh` (with rotation)
- [ ] `AuthController`: `POST /api/auth/logout`
- [ ] Admin seed `CommandLineRunner` (env-var driven, only runs if no admin exists)
- [ ] `PATCH /api/users/{id}/role` promote-to-admin endpoint (`ADMIN`-only)
- [ ] Lock down `ProductController`/`ImageController` write endpoints to `ADMIN`
- [ ] Tests: register/login happy path, bad credentials, expired/invalid token rejected, refresh rotation, role-gated endpoint rejects `BUYER`
