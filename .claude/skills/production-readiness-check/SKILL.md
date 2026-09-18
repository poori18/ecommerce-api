---
name: production-readiness-check
description: Run a production readiness check on this Spring Boot service before a release, deploy, or go-live — covering config/secrets hygiene, logging and observability, and error-handling/API contracts, plus live checks against a running instance where possible. Use this whenever the user asks if the app is "ready for prod", "ready to deploy", "ready to ship", wants a "go-live checklist", a "pre-launch check", a "release checklist", or a general health check of the service before release — even if they don't say "production readiness" explicitly.
---

# Production Readiness Check

A Spring Boot service that works in dev can still fail on its first day in production —
usually not from missing features, but from things dev environments quietly tolerate:
a password sitting in a committed properties file, `ddl-auto=update` silently altering
a real schema, SQL logging flooding prod logs, or a stack trace leaking straight into
an API response. This check exists to catch exactly that category of problem, before
it becomes an incident.

Work through the three static categories below, then the live checks. Each item gets a
status — ✅ pass, ⚠️ warning, ❌ fail — and a one-line reason. Don't just report status;
say *why* it matters and what to change, since that's what makes the report actionable
instead of a wall of checkmarks.

## 1. Config & secrets hygiene

- Search every tracked `application*.properties` / `application*.yml` for things that
  look like real credentials (passwords, API keys, connection strings with embedded
  auth). A value is fine if it's a placeholder, an environment variable reference
  (`${DB_PASSWORD}`), or lives in a file `.gitignore` actually excludes — confirm the
  exclusion by checking `.gitignore`, don't assume.
- Find the schema-management setting (Hibernate `ddl-auto`, Flyway/Liquibase mode,
  etc.) for every profile. `create`, `create-drop`, or `update` are dev-only — a
  profile that could plausibly run against production should use `validate` or
  `none`. Flag anything else and name the profile it's in.
- Check whether datasource URL/credentials for anything resembling a prod profile are
  externalized (env vars, a secrets manager, a gitignored local file) rather than
  hardcoded in a tracked file.

## 2. Logging & observability

- Look for DEBUG/TRACE-level logging on framework internals (`org.hibernate.SQL`,
  `show-sql=true`, request/response body logging) in any profile not clearly scoped to
  local dev. This is fine in `application-local.properties`; it's a problem if it's in
  the default `application.properties` with no profile guard.
- If Spring Boot Actuator is on the classpath, check `management.endpoints.web.exposure.include`
  — exposing everything (`*`) means `/actuator/env`, `/actuator/heapdump`, etc. are
  reachable, which can leak secrets and memory contents. Recommend an explicit allowlist
  (typically `health,info`) for anything internet-facing.
- Confirm there's a way to check the app is alive (health endpoint or equivalent) —
  note if there isn't one.

## 3. Error handling & API contracts

- Find the centralized exception handler (e.g. `@RestControllerAdvice`) and cross-check
  it against the custom exceptions actually thrown in the service layer — an exception
  type with no handler mapping will fall through to a generic 500 with whatever default
  body Spring produces, which usually is not what you want in an API response.
- Check `server.error.include-stacktrace` and `server.error.include-message` — `always`
  on either is a leak in prod; recommend `on-param` or `never`.
- Spot-check that validation failures (`@Valid` violations) return a structured 400 with
  field-level detail, not a raw framework exception body.

## 4. Live checks (only if the app is reachable)

Try hitting the running instance before assuming it's down — check `application*.properties`
for the configured port rather than guessing 8080, since it's common for local dev to run
on an alternate port.

- Hit the health endpoint: expect 200 and a healthy status.
- Trigger one known error path (e.g. request a resource by an id that doesn't exist) and
  confirm the response body matches what section 3 expects — no stack trace, structured
  JSON.
- If Swagger/OpenAPI UI is enabled, note it explicitly: fine for an internal/staging
  environment, worth flagging if this build is meant to be public-facing prod.
- If the app isn't running, say so plainly, skip this section, and mention the run
  command from the project's CLAUDE.md rather than guessing one.

## Output

Report the checklist in the conversation, grouped by the four sections above, each item
as `status — what you checked — why it matters (if not a plain pass)`.

Also save the same content as a Markdown file at
`.claude/reports/production-readiness-<YYYY-MM-DD-HHmm>.md` (create the directory if it
doesn't exist) so the user has a point-in-time record they can diff against a later run.
Mention the file path when you're done. These reports are transient artifacts, not
project documentation — if `.claude/reports/` isn't already in `.gitignore`, add it and
say so, rather than leaving generated reports to pile up in version control.
