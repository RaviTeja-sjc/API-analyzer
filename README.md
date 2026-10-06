# API Analyzer

An enterprise-grade, full-stack platform designed to detect semantic breaking changes in OpenAPI specifications, trace their impact down to the exact lines of code in your Java codebase, and automatically generate migration patches.

## 1. The Problem
Microservice architectures frequently suffer from "API breakage"—where a backend team alters an endpoint (e.g., removing a field, changing a type) without realizing that downstream consumers rely on that exact structure. Traditional tools only diff OpenAPI specs, leaving engineers to manually hunt through hundreds of repositories to find and fix the affected code.

## 2. The Solution & Features
API Analyzer bridges the gap between API contracts and physical source code.
- **Semantic Diffing:** Deep comparison of OpenAPI specs detecting non-breaking (additions) vs. breaking (removals, type changes) alterations.
- **AST Source Analysis:** Parses raw Java source code to build an Abstract Syntax Tree (AST), tracking exactly which controllers and methods consume which endpoints.
- **Impact Graph Visualization:** A stunning React Flow interface mapping the blast radius of a breaking change from Endpoint -> Controller -> Service -> Method.
- **Automated Migration Patches:** Generates immutable code diffs and migration suggestions that developers can review, approve, and apply.
- **Continuous Integration:** Native GitHub Webhook integration for automatic PR commenting.

## 3. Architecture
Built using a robust Modular Monolith design to prevent domain leakage while allowing future microservice extraction.
- **Backend:** Java 21, Spring Boot 3.2, Maven, Hibernate, PostgreSQL (Supabase)
  - `module-core`: Security, Global Exceptions, DB Config
  - `module-ingestion`: GitHub Webhooks, Spec Parsing
  - `module-analysis`: AST Parsing, Rule Engines, Graph Traversal
  - `app-runner`: Application bootstrap
- **Frontend:** React, TypeScript, Vite, React Flow, TailwindCSS/Vanilla CSS.
- **Deployment:** Docker Multi-stage, Render Blueprint, GitHub Actions.

## 4. Setup & Local Development
1. Clone the repository.
2. In `backend/`, copy `.env.example` to `.env` and fill in local variables.
3. Start the backend: `mvn clean install` then run the `app-runner` main class. (Uses `application-dev.yml` defaults).
4. Start the frontend: `cd frontend`, `npm install`, `npm run dev`.

## 5. Environment Variables & Supabase
**Never commit secrets.** The application enforces a fail-fast startup in production.
- `DATABASE_URL`: Supabase PostgreSQL connection string.
- `DATABASE_USER` / `DATABASE_PASSWORD`: Supabase credentials.
- `JWT_SECRET`: 256-bit secure hash for authentication.
- `DB_ENCRYPTION_KEY`: 32-byte AES key for encrypting GitHub tokens at rest.
- `CORS_ALLOWED_ORIGINS`: Allowed frontend origins.

## 6. API Analysis & Breaking Change Rules
The `ApiDiffEngine` evaluates sequential OpenAPI specs against strict rules:
- `EndpointRemovedRule`: Flags if a URI path or HTTP method disappears.
- `TypeChangedRule`: Flags if a schema type mutates (e.g., String -> Integer).
- `FieldRemovedRule`: Flags if required request/response properties are dropped.

## 7. Source Analysis & Migration Workflow
- **Parsing:** `JavaAstParserService` uses `javaparser-core` via parallel streams to digest raw code.
- **Graphing:** `ImpactGraphBuilderService` constructs a dependency tree avoiding O(N²) explosion via HashSet cycle detection.
- **Migration:** The `MigrationEngineService` maps breaking API changes to AST nodes, proposing explicit Java code replacements via the React `MigrationReviewView`.

## 8. GitHub Integration
The `GithubWebhookController` listens for PR events. It cryptographically validates the `X-Hub-Signature-256` payload and runs the analysis pipeline asynchronously, eventually posting an idempotent impact report comment back to the GitHub PR.

## 9. Testing
- **Backend:** JUnit 5 tests covering the DiffEngine and AST parser logic.
- **Frontend:** Vitest and `@testing-library/react` for component validation.
- **Smoke Tests:** Playwright E2E scripts (`smoke.spec.ts`) validate the full Browser -> Render -> Supabase workflow.

## 10. Docker & Render Deployment
- **Docker:** A highly optimized multi-stage `Dockerfile`. Uses `maven` builder and `eclipse-temurin:21-jre-alpine` runtime. Executes as a secure non-root `appuser`.
- **Render (`render.yaml`):** Provides Infrastructure-as-Code.
  - Automatically provisions the Docker backend and static React frontend.
  - Health checks bound to Spring Boot `/actuator/health/liveness`.

## 11. Security & Resilience
- **Database:** Supabase connections fortified via HikariCP (`max-lifetime: 30m`, `keepalive`) to survive firewall drops.
- **Secrets:** Passwords BCrypt hashed. GitHub PATs AES-encrypted at rest.
- **Web:** Global Exception handlers scrub stack traces. React UI utilizes Axios interceptors for 10-second timeouts and `ErrorBoundary` components to gracefully handle malformed data crashes.

## 12. Troubleshooting
- **Missing Vitest Types:** Run `npm install` and restart the TS server.
- **DB Connection Drops:** Ensure `application-prod.yml` HikariCP settings align with your Supabase tier limits (Max pool size: 15).
- **GitHub Webhook Fails:** Verify the webhook secret in GitHub matches the backend environment variable.

## 13. Limitations & Future Improvements
- **Multi-Language Support:** Currently strictly analyzes Java code. Future scope includes Python (AST) and TypeScript/Node.js AST ingestion.
- **Auto-Committing Patches:** Currently, migrations require manual copy-pasting. Future scope includes utilizing the GitHub API to directly push migration commits to consumer repositories.
- **Scale:** Extremely massive mono-repos (10,000+ files) may require extracting the `JavaAstParserService` into a dedicated horizontally scaled worker cluster using Kafka.
