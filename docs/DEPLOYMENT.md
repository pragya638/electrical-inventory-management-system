# Deployment Guide

This guide describes how to build, configure and run the **Electrical Inventory &
Stock Management System** prototype in another environment. It is written for an
internship-level prototype, **not** for a hardened production rollout.

> **Reminder:** All seeded company, plant, supplier, product and user records are
> **demonstration data only** and must be replaced before any real use.

---

## 1. Prerequisites

| Requirement | Version / notes |
| --- | --- |
| Java (JDK) | **17** or newer (the project targets Java 17 bytecode) |
| Maven | Not required — the **Maven Wrapper** (`mvnw` / `mvnw.cmd`) is bundled |
| MySQL | **8.x** (or a MySQL-compatible server) |
| Build tool (optional) | Maven 3.9+ if you prefer a local install over the wrapper |

Verify Java:

```bash
java -version
```

## 2. Maven build command

Build and test from the project root:

```bash
# Windows
.\mvnw.cmd clean package

# macOS / Linux
./mvnw clean package
```

To run the tests only (uses in-memory H2, **no MySQL required**):

```bash
.\mvnw.cmd clean test        # Windows
./mvnw clean test            # macOS / Linux
```

The build produces an executable JAR in `target/`:

```
target/inventory-management-system-1.0.0.jar
```

To skip tests during a build (not recommended, but sometimes useful for a quick
artifact): append `-DskipTests`.

## 3. Required environment variables

Database credentials are **never** stored in source. Provide them via environment
variables (or your platform's secret/config store). Do **not** commit real values.

| Variable | Required | Default | Purpose |
| --- | --- | --- | --- |
| `DB_URL` | Optional | `jdbc:mysql://localhost:3306/wonder_electricals_inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true` | JDBC connection URL |
| `DB_USERNAME` | Optional | `root` | MySQL username |
| `DB_PASSWORD` | **Required in practice** | *(empty)* | MySQL password — provide it, never commit it |
| `APP_SEED_ENABLED` | Optional | `true` | Load demonstration data on first start |
| `PORT` | Optional (platform-dependent) | `8080` | HTTP port (some hosts inject `PORT`) |

Windows PowerShell:

```powershell
$env:DB_URL      = "jdbc:mysql://<host>:3306/<database>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
$env:DB_USERNAME = "<db-user>"
$env:DB_PASSWORD = "<db-password>"
$env:APP_SEED_ENABLED = "true"
```

macOS / Linux:

```bash
export DB_URL="jdbc:mysql://<host>:3306/<database>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
export DB_USERNAME="<db-user>"
export DB_PASSWORD="<db-password>"
export APP_SEED_ENABLED="true"
```

## 4. MySQL configuration

1. Ensure a MySQL 8 server is running and reachable from the application host.
2. Create a dedicated application user (recommended) with privileges on the target
   database. Example (run as a MySQL admin) — replace placeholders with your own values:

   ```sql
   CREATE DATABASE wonder_electricals_inventory
     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

   CREATE USER 'inventory_app'@'%' IDENTIFIED BY '<strong-password>';
   GRANT ALL PRIVILEGES ON wonder_electricals_inventory.* TO 'inventory_app'@'%';
   FLUSH PRIVILEGES;
   ```

   Then set `DB_USERNAME=inventory_app` and `DB_PASSWORD=<strong-password>`.

3. The default `DB_URL` includes `createDatabaseIfNotExist=true`, so the database is
   created automatically **if the user has permission**. If you prefer to manage the
   schema/database yourself, create it ahead of time (as above) and remove that flag.

Notes:

- `useSSL=false&allowPublicKeyRetrieval=true` are convenient for local/dev use over a
  trusted network. For a real deployment, enable TLS and set `useSSL=true` with the
  correct server certificate, and remove `allowPublicKeyRetrieval=true`.
- Charset `utf8mb4` is recommended for full Unicode support.

## 5. Spring Boot configuration

The relevant settings live in `src/main/resources/application.properties` and are safe
to commit (no secrets). You can override any of them with environment variables or
`--property=value` arguments without editing the file.

| Property | Value (default) | Meaning |
| --- | --- | --- |
| `spring.datasource.url` | `${DB_URL:...}` | Connections string, overridable via `DB_URL` |
| `spring.datasource.username` | `${DB_USERNAME:root}` | DB user, overridable via `DB_USERNAME` |
| `spring.datasource.password` | `${DB_PASSWORD:}` | DB password, overridable via `DB_PASSWORD` |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate creates/extends tables on start-up |
| `spring.jpa.open-in-view` | `false` | Session closed at request end; DTO mapping happens in-service |
| `app.seed.enabled` | `${APP_SEED_ENABLED:true}` | Demo data seeding toggle |
| `spring.thymeleaf.cache` | `false` | Template caching off (dev convenience) |

**Production-oriented recommendations (configuration only, no code change needed):**

- Change `spring.jpa.hibernate.ddl-auto` from `update` to **`validate`** and manage
  schema changes with migration scripts (e.g. Flyway/Liquibase) instead of Hibernate.
- Set `spring.thymeleaf.cache=true`.
- Turn **off** demo seeding: `APP_SEED_ENABLED=false`.
- Configure TLS for the database connection.

Example overrides via command line:

```bash
java -jar target/inventory-management-system-1.0.0.jar \
  --server.port=9090 \
  --spring.jpa.hibernate.ddl-auto=validate \
  --app.seed.enabled=false
```

## 6. How to build the application

```bash
# Windows
.\mvnw.cmd clean package

# macOS / Linux
./mvnw clean package
```

Output: `target/inventory-management-system-1.0.0.jar` (an executable Spring Boot JAR
with an embedded Tomcat server).

## 7. How to run the application

**During development (Spring Boot Maven plugin):**

```bash
.\mvnw.cmd spring-boot:run     # Windows
./mvnw spring-boot:run         # macOS / Linux
```

**As a packaged JAR (recommended for deployments):**

```bash
java -jar target/inventory-management-system-1.0.0.jar
```

Then open <http://localhost:8080> and sign in with a demo account:

| Email | Password | Role |
| --- | --- | --- |
| `admin@demo.local` | `Admin@123` | ADMIN |
| `manager@demo.local` | `Admin@123` | INVENTORY_MANAGER |
| `viewer@demo.local` | `Admin@123` | VIEWER |

To run in the background on Linux:

```bash
nohup java -jar target/inventory-management-system-1.0.0.jar > app.log 2>&1 &
```

## 8. PORT configuration for cloud deployment

The embedded server listens on **8080** by default. Most platforms (Heroku, Render,
Railway, Azure App Service, container hosts) provide the port through a `PORT`
environment variable. Set the Spring Boot port to match, either with `SERVER_PORT`
(Spring's relaxed binding maps `SERVER_PORT` → `server.port`) or by passing the flag.

```bash
# Uses the platform-provided PORT if set
SERVER_PORT=$PORT java -jar target/inventory-management-system-1.0.0.jar
```

```bash
# Explicit port
java -jar target/inventory-management-system-1.0.0.jar --server.port=9090
```

Container/Docker note: expose the same port as `server.port` (e.g. `EXPOSE 8080`), and
make sure the application binds to `0.0.0.0` (the default for Spring Boot) rather than
only `localhost`.

## 9. Database setup requirements

- A reachable MySQL 8 instance, reachable from the application host/network.
- A database (default name `wonder_electricals_inventory`) and a user with privileges
  on it. `createDatabaseIfNotExist=true` can create the database for you if the user is
  allowed to do so.
- First start with `app.seed.enabled=true` (default) seeds demonstration data and the
  three demo users. It runs **only when the users table is empty**, so restarts do not
  duplicate or overwrite data.
- For an empty start, set `APP_SEED_ENABLED=false` and create users/master data yourself
  through the ADMIN account.
- Schema is created/extended by Hibernate while `ddl-auto=update`. For real deployments,
  switch to `validate` and manage the schema with migrations.

## 10. Deployment considerations

- **This is a prototype.** Before any real use, replace demonstration data, set strong
  credentials, and review security and operations. None of the seeded records are real.
- **Secrets:** keep `DB_PASSWORD` (and any session/config secrets) in environment
  variables or the platform's secret manager — never in `application.properties`,
  `README.md`, this guide, or Git.
- **Timezone:** the default connection URL pins `serverTimezone=UTC`. Adjust deliberately
  if your business requires a specific timezone.
- **Session state:** the app uses in-memory HTTP sessions. A single instance is fine; a
  multi-instance deployment would need sticky sessions or a shared session store.
- **Health/monitoring:** consider adding Spring Boot Actuator for health checks and
  metrics before a managed deployment (not currently included).
- **Reverse proxy / TLS:** terminate HTTPS at a reverse proxy and forward to the app port.
  Ensure `X-Forwarded-*` headers are handled if you add one.
- **Resources:** a small instance is sufficient for the demonstration workload. MySQL is
  the only external dependency required at runtime.
- **Backups & migrations:** for anything beyond a demo, back up the database and adopt a
  migration tool rather than `ddl-auto=update`.
- **CSRF/API:** the REST API under `/api/**` is exempt from CSRF and authenticates with a
  session cookie. Keep this in mind if you expose it more broadly.

---

## Quick reference

```bash
# 1. Configure
export DB_URL="jdbc:mysql://<host>:3306/<database>?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
export DB_USERNAME="<db-user>"
export DB_PASSWORD="<db-password>"
export APP_SEED_ENABLED="true"

# 2. Build
./mvnw clean package

# 3. Run
java -jar target/inventory-management-system-1.0.0.jar
```
