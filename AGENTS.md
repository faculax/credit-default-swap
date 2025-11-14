
---

# 🤖 Agents Guide

Welcome to the **Agents’ Collective** — our space for collaborative, focused, and fun coding sessions.
This document sets the tone and gives you the essentials to dive in quickly.

---

## 🎨 Application Look & Feel
- **Fonts**: Arial, Georgia  
- **Colours**:  
  - RGB(255, 255, 255)  
  - RGB(0, 240, 0)  
  - RGB(60, 75, 97)  
  - RGB(0, 232, 247)  
  - RGB(30, 230, 190)  
  - RGB(0, 255, 195)  

---

## 🌱 Vibes

* **Collaborate > Isolate** → ask, share, pair.
* **Consistency > Creativity (in scaffolding)** → when in doubt, follow the patterns.
* **Flow > Formality** → small iterations, working demos, quick feedback.
* **Simplicity > Cleverness** → clean, understandable solutions win.

---

## 🏗️ Service Architecture

We’re building **Spring Boot Java services**, stitched together with **Postgres** for persistence.
Each service lives as its own folder/module under the root project.

* **Framework**: Spring Boot
* **Database**: PostgreSQL
* **Infrastructure**: Docker Compose at the root level orchestrates everything

---

## 📦 Adding a New Service

When creating a new backend service, **don’t start from scratch** — follow these steps:

1. **Choose a template service**

   * Pick an existing service that feels closest to what you need.
   * Copy its structure and configs.

2. **Update identifiers**

   * Rename the service module, package names, and main application class.
   * Adjust service name in `application.yml`.

3. **Add Postgres schema (if needed)**

   * Update the database config for your service in `docker-compose.yml`.
   * Apply schema migrations (`flyway` or SQL init scripts if used).

4. **Register in Docker Compose**

   * Define your new service container.
   * Add dependencies (e.g., Postgres).
   * Make sure ports don’t clash with existing ones.

5. **Test integration**

   * Run `docker-compose up --build` at root level.
   * Verify your new service spins up and connects to its DB.

---

## ⚡ Vibe Session Rituals

* **Kick-off (5 mins):** quick sync — what’s today’s focus?
* **Deep Work (25–40 mins):** silent or paired coding.
* **Checkpoint (5–10 mins):** share progress, blockers, fun hacks.
* **Iterate:** repeat cycles until wrap-up.
* **Close (5 mins):** commit, push, and celebrate wins 🎉.

---

## 🛠️ Useful Commands

Spin up all services:

```sh
docker-compose up --build
```

Spin down everything:

```sh
docker-compose down -v
```

Run a single service locally:

```sh
./mvnw spring-boot:run
```

---

## 🧭 Principles

* **Consistency is a feature.** New services should feel like old services.
* **Infrastructure is shared.** Don’t reinvent; extend the Docker Compose.
* **Documentation beats memory.** Update this file when workflows change.
* **Keep it light.** The goal is flow, not bureaucracy.

---

Would you like me to **add a “Service Template Checklist” section** (with filenames/configs to touch when cloning an existing service), so new agents don’t miss any step when scaffolding?

---

## 📋 Service Template Checklist (Clone & Rename)

Use this checklist whenever you spin up a new Spring Boot service by copying an existing one. Tick every box before opening a PR.

### 1. Module & Build Basics
- [ ] Copy existing service folder (e.g. `backend/` or similar template) and rename to your new service name (kebab-case for folder, lowerCamel or PascalCase for main class as per convention).
- [ ] Update `pom.xml`:
   - [ ] `<artifactId>` to new service name
   - [ ] `<name>` / `<description>`
   - [ ] Check Java version & dependency alignment with other services
- [ ] Ensure any hard‑coded package paths inside `pom.xml` plugins (e.g. Spring Boot repackage) still match.

### 2. Package & Main Class
- [ ] Rename base Java package (e.g. `com.example.oldservice` → `com.example.newservice`).
- [ ] Rename main application class (`OldServiceApplication` → `NewServiceApplication`).
- [ ] Update any `@ComponentScan` / `@EntityScan` annotations if they reference old package roots.

### 3. Configuration & Properties
- [ ] Duplicate or create `src/main/resources/application.yml` (or `.properties`).
- [ ] Set unique `spring.application.name`.
- [ ] Configure database connection (JDBC URL, username, password, schema). Prefer environment variables in Docker Compose over hard‑coded values.
- [ ] Add any required feature flags or service‑specific properties.

### 4. Database & Migrations
- [ ] Create schema migrations (`flyway` or Liquibase) in `src/main/resources/db/migration`.
- [ ] Verify schema name / search path matches Postgres setup.
- [ ] Add seed / reference data if needed (keep idempotent).

### 5. Docker & Compose
- [ ] Create or adapt `Dockerfile` (match base image + JVM opts pattern used elsewhere).
- [ ] Add service block to `docker-compose.yml`:
   - [ ] Unique container name & service key
   - [ ] Build context points to new module
   - [ ] Port mapping doesn’t clash (e.g. 8080, 8081 … pick next free)
   - [ ] Environment vars: DB creds, `SPRING_PROFILES_ACTIVE`, any API keys (use `.env` or secrets – never commit sensitive values)
   - [ ] Depends_on Postgres (and any other required services)
- [ ] Add volume mounts only if truly needed (logs, temp, uploads). Keep minimal.

### 6. Observability & Ops
- [ ] Health endpoint (`/actuator/health`) reachable.
- [ ] Metrics if standardized (Prometheus via actuator `/actuator/prometheus`).
- [ ] Logging pattern follows existing services (JSON vs text; correlation IDs if adopted).

### 7. Security & Validation
- [ ] If security is enabled globally, add necessary `WebSecurityConfigurerAdapter` / filter chain updates.
- [ ] Input validation (DTO constraints with `javax.validation` annotations).
- [ ] Sanitise external inputs (headers, query params) if service is externally exposed.

### 8. Testing
- [ ] Unit tests for core logic/services (`@ExtendWith(MockitoExtension.class)`).
- [ ] Contract tests for external integrations (wiremock or mocked clients).
- [ ] Integration tests (`@SpringBootTest`) tagged appropriately (e.g. `@Tag("integration")`).
- [ ] Update any shared test utilities (factories/builders) with new domain objects.
- [ ] Ensure tests run green locally: `./mvnw test`.

### 9. Documentation & Metadata
- [ ] Add README in new service folder: purpose, API surface, local run instructions.
- [ ] Update root `AGENTS.md` if introducing new patterns or environment variables.
- [ ] Add OpenAPI generation if standard in project (springdoc config).

### 10. CI & Coverage
- [ ] Ensure service is picked up by existing multi‑module Maven build.
- [ ] JaCoCo thresholds respected or temporarily adjusted with rationale.
- [ ] Service test classification (unit/contract/integration) fits reporting pipeline.

### 11. Runtime Verification
- [ ] Run: `docker-compose up --build` and confirm service starts + connects to Postgres.
- [ ] Smoke test primary endpoint (e.g. `curl http://localhost:<port>/actuator/health`).
- [ ] Verify schema objects exist (tables, indexes) in Postgres container.

### 12. Cleanup & Consistency
- [ ] Remove any leftover references to old service name.
- [ ] No unused classes / placeholder code.
- [ ] Align code style and formatting with existing modules.

### 13. Optional Enhancements
- [ ] Add data lineage hooks if applicable (see lineage docs in `docs/`).
- [ ] Add correlation / tracing IDs if cross‑service tracing is enabled.
- [ ] Register service in service discovery / gateway routing if required.

---

If any step feels ambiguous, ask early; consistency across services is a shared asset.

