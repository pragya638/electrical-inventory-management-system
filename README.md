# Electrical Inventory & Stock Management System

An **internship-level prototype** of a web application for managing electrical
products, plants, suppliers and per-plant stock levels. It demonstrates a complete
Stock IN / Stock OUT workflow, low-stock monitoring, reporting with CSV export,
role-based access control and a REST API, built with Spring Boot and Thymeleaf.

> **Important:** This is a learning/prototype project, **not** a production system.
> Every company, plant, supplier, product, user and stock figure that ships with or
> is seeded by the application is **demonstration data invented for the prototype**.
> Nothing in this repository represents real records of any operating company.

---

## Table of contents

- [Project description](#project-description)
- [Main features](#main-features)
- [User roles](#user-roles)
- [Technology stack](#technology-stack)
- [System architecture](#system-architecture)
- [Main modules](#main-modules)
- [Stock IN workflow](#stock-in-workflow)
- [Stock OUT workflow](#stock-out-workflow)
- [Low-stock logic](#low-stock-logic)
- [Reports and CSV export](#reports-and-csv-export)
- [Validation and security](#validation-and-security)
- [Testing](#testing)
- [Local setup](#local-setup)
- [Required environment variables](#required-environment-variables)
- [Demo credentials](#demo-credentials)
- [How to run using Maven](#how-to-run-using-maven)
- [Project structure](#project-structure)

---

## Project description

The system tracks electrical inventory (cables, wires, switches, accessories and
lighting products) across multiple plants. Stock can only change through explicit
**Stock IN** and **Stock OUT** operations, each of which writes an audit transaction.
Stock levels are compared against a per-product **reorder level** to flag low-stock
and out-of-stock situations. Users sign in and see only the actions their role allows.

It is intentionally a self-contained monolith: Spring MVC pages for humans, a JSON
REST API for integrations/scripts, and a MySQL database. It is meant to show good
practice (DTOs, validation, transactional service layer, role-based security,
automated tests) on a realistic but small domain.

## Main features

- Session-based login with BCrypt-hashed passwords.
- Three roles: `ADMIN`, `INVENTORY_MANAGER`, `VIEWER`.
- Master data management: **categories, plants, suppliers, products** (create, edit,
  activate/deactivate, search/filter).
- Per-plant inventory with current quantity and last-updated timestamp.
- **Stock IN** (receipt) and **Stock OUT** (issue) movements with reference number,
  remarks and the username of the person who performed them.
- Immutable stock transaction history with filtering by direction and date range.
- **Low-stock** and **out-of-stock** detection against each product's reorder level.
- Dashboard with totals and per-plant stock summary.
- Six reports and **CSV export** of inventory, low stock and stock movements.
- User management (ADMIN only).
- REST API mirroring the main operations.
- Consistent error handling (400 / 401 / 403 / 404 / 405 / 409 / 415) for pages and API.
- Automated test suite (127 tests) that runs without a database server.

## User roles

| Role | Capabilities |
| --- | --- |
| **ADMIN** | Full access. Manage users, master data, stock movements, dashboard and reports. Only role allowed to **delete** master data and to access `/users/**` and `/api/users/**`. |
| **INVENTORY_MANAGER** | Create/edit master data and perform Stock IN / Stock OUT; view dashboard and reports. Cannot manage users or delete master data. |
| **VIEWER** | Read-only. Can view dashboard, inventory, low-stock, reports and CSV exports. Cannot open add/edit forms or perform any write operation. |

Access is enforced server-side in `SecurityConfig` and reflected in the navigation
using Thymeleaf Spring Security (`sec:authorize`) so links a role cannot use are hidden.

## Technology stack

| Layer | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.5.7 |
| Web / View | Spring MVC, Thymeleaf, Thymeleaf Extras Spring Security 6 |
| Security | Spring Security (form login + session, BCrypt) |
| Persistence | Spring Data JPA / Hibernate |
| Database | MySQL 8 (production/dev), H2 in-memory (tests only) |
| Validation | Jakarta Bean Validation (Hibernate Validator) |
| Build | Maven (via Maven Wrapper `mvnw` / `mvnw.cmd`) |
| Front end | HTML, Bootstrap 5, minimal vanilla JavaScript |
| Testing | JUnit 5, Spring Boot Test, MockMvc, Mockito, Spring Security Test |

## System architecture

Classic layered (n-tier) monolith:

```
Browser / REST client
        |
        v
Spring Security filter chain  (authentication, authorization, CSRF)
        |
        v
Controllers  ---  Thymeleaf page controllers   +   REST API controllers (/api/**)
        |
        v
Services   ---  business rules, transactions, DTO <-> entity mapping
        |
        v
Repositories  ---  Spring Data JPA interfaces
        |
        v
Hibernate  --->  MySQL database
```

- **Controllers** never touch entities for stock changes; they translate requests to
  DTOs and call services. API controllers return JSON; page controllers return views.
- **Services** own all business rules. `StockService` is the *only* code path allowed
  to change stock quantities, and each movement writes the inventory row and its audit
  transaction inside **one database transaction**.
- **Repositories** are Spring Data JPA interfaces; entities map to tables.
- **Security** is a single `SecurityFilterChain` that answers page requests with
  redirects and `/api/**` requests with JSON `401`/`403`.
- **DTOs** isolate the web/API layer from entities; entities are never serialized
  directly. `spring.jpa.open-in-view=false` confirms mapping happens inside the
  service transaction.

## Main modules

| Module | Package | Responsibility |
| --- | --- | --- |
| Authentication & Users | `controller`, `security`, `service.UserService` | Login/logout, user CRUD (ADMIN), password hashing |
| Master Data | `controller`, `service` (`CategoryService`, `PlantService`, `SupplierService`, `ProductService`) | CRUD and activation of categories, plants, suppliers, products |
| Inventory | `service.InventoryService`, `entity.Inventory` | Current quantity per product/plant and status |
| Stock Movements | `service.StockService`, `entity.StockTransaction` | Stock IN / OUT, audit trail |
| Transactions | `service.TransactionService` | Query stock transaction history and reports |
| Dashboard | `service.DashboardService` | Aggregate totals and plant-wise summary |
| Reports | `service.ReportService`, `util.CsvExporter` | Report data and CSV generation |
| REST API | `controller.api` | JSON equivalents of the page operations |
| Error handling | `exception.GlobalExceptionHandler` | Uniform API/page error responses |

## Stock IN workflow

Used when goods are received (e.g. a GRN). Implemented in
`StockService.stockIn(StockMovementRequest)`:

1. A page form (`/stock`) or API request (`POST /api/stock/in`) submits `productId`,
   `plantId`, `quantity`, optional `referenceNumber` and `remarks`.
2. Bean Validation checks the payload (`productId`/`plantId` positive, `quantity` > 0,
   field length limits).
3. The product and plant must exist and be **active**.
4. The matching inventory row is loaded, or **created** if this product has never been
   stocked at that plant.
5. The quantity is increased and saved.
6. A `StockTransaction` of type `IN` is written, recording the quantity, reference,
   remarks, timestamp and the signed-in username.
7. Steps 5 and 6 commit together in one transaction; any failure rolls both back.

## Stock OUT workflow

Used when material is issued to a plant/site. Implemented in
`StockService.stockOut(StockMovementRequest)`:

1. A form (`/stock`) or API request (`POST /api/stock/out`) submits the same payload.
2. Validation and active product/plant checks run as for Stock IN.
3. The existing inventory row is required; there is nothing to issue if it does not exist.
4. **Over-issue is rejected:** if `quantity > available`, an `InsufficientStockException`
   is thrown (HTTP `409` / a flash error on the page) and **nothing is written** — stock
   can never go negative.
5. Otherwise the quantity is decreased and saved.
6. A `StockTransaction` of type `OUT` is written with the same audit fields.
7. Quantity update and transaction record commit together.

## Low-stock logic

The rule lives in one place, `StockStatus.of(Inventory)`:

- `quantity <= 0` → **OUT_OF_STOCK**
- otherwise `quantity <= product.reorderLevel` → **LOW_STOCK**
- otherwise → **IN_STOCK**

The low-stock view, dashboard and low-stock report/CSV all use this same rule, so they
can never disagree. The shortfall shown in the low-stock report is
`reorderLevel - quantity`.

## Reports and CSV export

`ReportService` builds the following, served both as pages (`/reports/...`) and as
CSV downloads (`.csv` endpoints):

1. **Current inventory** — every product/plant row with quantity, unit, reorder level,
   status and last-updated time.
2. **Plant-wise inventory** — stock grouped per plant.
3. **Low-stock report** — only rows at or below reorder level, with shortfall.
4. **Stock IN report** — receipts, optionally filtered by date range.
5. **Stock OUT report** — issues, optionally filtered by date range.
6. **Transaction history** — all movements, optionally filtered by date range.

CSV is produced by `util.CsvExporter` with quoted, escaped values and a header row so
the files open cleanly in spreadsheets.

## Validation and security

**Validation**

- Jakarta Bean Validation on all request DTOs (`@NotBlank`, `@NotNull`, `@Positive`,
  `@PositiveOrZero`, `@Size`).
- Page forms re-render with field errors; API requests receive a JSON error body.
- Service-level defence in depth: stock quantities are re-checked as positive, and
  inactive products/plants cannot be moved.
- `GlobalExceptionHandler` maps errors consistently: `400` (bad/malformed input),
  `401`/`403` (auth), `404` (missing resource/route), `405` (wrong HTTP method, with an
  `Allow` header), `409` (duplicate resource, insufficient stock), `415` (unsupported
  media type).

**Security**

- Spring Security form login with session cookies; passwords hashed with **BCrypt**.
- **Role-based authorization** for every route group (see [User roles](#user-roles)).
- CSRF protection is enabled for the Thymeleaf forms. It is skipped only for
  `/api/**`, which authenticates with a session cookie so no ambient credentials apply.
- Login failure does not reveal whether an email exists.
- Page requests that fail auth redirect to `/login`; `/api/**` returns JSON `401`/`403`.
- No credentials are stored in `application.properties`; the database password is read
  from an environment variable.

## Testing

The suite is run with the Maven Wrapper and uses an **in-memory H2** test profile
(`src/test/resources/application.properties`), so **no MySQL server is required** to run
the tests. Demo seeding is disabled in tests and each test sets up its own fixtures.

Current result:

```
Tests run: 127, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Coverage includes: service-level business rules (`StockService`, `UserService`,
`ProductService`, `ReportService`, `MasterDataService`), DTO validation, CSV export,
page rendering, authentication, full screenshot-style UI/API contract integration tests
(role matrix, malformed inputs → `400`, unknown routes → `404`, wrong method → `405`,
unsupported media type → `415`, CSRF behaviour).

Run the tests with:

```bash
# Windows
.\mvnw.cmd clean test

# macOS / Linux
./mvnw clean test
```

## Local setup

**Prerequisites**

- **JDK 17** (or newer that is compatible with Java 17 bytecode).
- **MySQL 8** running locally (or reachable via `DB_URL`).
- No global Maven install needed — the **Maven Wrapper** is included.

**Steps**

1. Clone the repository and open the project folder.
2. Make sure MySQL is running and that you know a username/password for it.
3. Set the environment variables (see the next section). The default JDBC URL uses
   `createDatabaseIfNotExist=true`, so the `wonder_electricals_inventory` database is
   created on first start; the MySQL user still needs permission to create it.
4. Start the application with `.\mvnw.cmd spring-boot:run` (Windows) or
   `./mvnw spring-boot:run` (macOS/Linux).
5. Open <http://localhost:8080> and sign in with a [demo credential](#demo-credentials).
6. On the first start the app seeds demonstration data automatically (see
   `app.seed.enabled` below). It only seeds when the users table is empty.

## Required environment variables

The application reads its database configuration and seeding flag from the environment.
**Never commit real credentials** — set them in your shell or deployment platform.

| Variable | Required | Example | Purpose |
| --- | --- | --- | --- |
| `DB_URL` | No (has default) | `jdbc:mysql://localhost:3306/wonder_electricals_inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true` | JDBC connection URL |
| `DB_USERNAME` | No (defaults to `root`) | `root` | MySQL username |
| `DB_PASSWORD` | **Yes** in practice | *(your own password — never commit it)* | MySQL password |
| `APP_SEED_ENABLED` | No (defaults to `true`) | `true` / `false` | Load demonstration data on first start |

Set them for the current shell:

```powershell
# Windows PowerShell
$env:DB_URL      = "jdbc:mysql://localhost:3306/wonder_electricals_inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "<your-mysql-password>"
$env:APP_SEED_ENABLED = "true"
```

```bash
# macOS / Linux
export DB_URL="jdbc:mysql://localhost:3306/wonder_electricals_inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true"
export DB_USERNAME="root"
export DB_PASSWORD="<your-mysql-password>"
export APP_SEED_ENABLED="true"
```

## Demo credentials

These accounts are created by the demonstration data seeder. **They are demo logins
only** and must not be used as-is for any real deployment.

| Email | Password | Role |
| --- | --- | --- |
| `admin@demo.local` | `Admin@123` | ADMIN |
| `manager@demo.local` | `Admin@123` | INVENTORY_MANAGER |
| `viewer@demo.local` | `Admin@123` | VIEWER |

## How to run using Maven

```bash
# Windows PowerShell
.\mvnw.cmd clean test          # run the test suite (H2, no MySQL needed)
.\mvnw.cmd clean package       # build the executable JAR in target/
.\mvnw.cmd spring-boot:run     # run the app on http://localhost:8080

# macOS / Linux
./mvnw clean test
./mvnw clean package
./mvnw spring-boot:run
```

Run the packaged JAR directly:

```bash
java -jar target/inventory-management-system-1.0.0.jar
```

Override the port if 8080 is taken:

```bash
# Windows PowerShell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=9090"

# or with the packaged JAR
java -jar target/inventory-management-system-1.0.0.jar --server.port=9090
```

## Project structure

```
Wonder-Electricals-Inventory-Management-System/
|-- .mvn/                         # Maven Wrapper configuration
|-- mvnw, mvnw.cmd                # Maven Wrapper launchers
|-- pom.xml                       # Build file (Spring Boot 3.5.7, Java 17)
|-- README.md
|-- docs/
|   `-- DEPLOYMENT.md             # Deployment / configuration guide
`-- src/
    |-- main/
    |   |-- java/com/wonder/electricals/inventory/
    |   |   |-- InventoryManagementApplication.java
    |   |   |-- config/           # SecurityConfig, DataSeeder
    |   |   |-- controller/       # Page controllers
    |   |   |   `-- api/          # REST API controllers (/api/**)
    |   |   |-- dto/              # Request/response DTOs, validation, filters
    |   |   |-- entity/           # JPA entities (User, Product, Plant, ...)
    |   |   |-- exception/        # Custom exceptions + GlobalExceptionHandler
    |   |   |-- repository/       # Spring Data JPA repositories
    |   |   |-- security/         # UserDetails, entry point, access-denied handler
    |   |   |-- service/          # Business logic (StockService, ReportService, ...)
    |   |   `-- util/             # CsvExporter, DtoMapper
    |   `-- resources/
    |       |-- application.properties
    |       |-- static/           # css, js
    |       `-- templates/        # Thymeleaf views
    `-- test/
        |-- java/com/wonder/electricals/inventory/   # JUnit 5 test suite
        `-- resources/application.properties         # H2 test profile
```

---

## Demonstration data disclaimer

All organisations, plants, suppliers, products, users and stock quantities included in
this project are **fictional demonstration data** created solely to exercise the
prototype. They do **not** represent real records of any company, and the project makes
no claim about any real inventory, facility or business operation. This is an
internship-level prototype intended for learning and demonstration only.
