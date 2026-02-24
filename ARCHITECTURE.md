# AnyChart Docs Engine - Architecture & Technical Reference

> Complete technical documentation for the AnyChart documentation generation engine.
> This document is intended for developers and AI agents continuing work on this project.

## Table of Contents

- [Overview](#overview)
- [Technology Stack](#technology-stack)
- [System Architecture](#system-architecture)
- [Operating Modes](#operating-modes)
- [Configuration](#configuration)
- [Generation Pipeline](#generation-pipeline)
- [Validation & Report System](#validation--report-system)
- [Notification System](#notification-system)
- [Web Application & Routing](#web-application--routing)
- [Admin Panel](#admin-panel)
- [Database Schema](#database-schema)
- [Offline ZIP Generation](#offline-zip-generation)
- [Staging vs Production](#staging-vs-production)
- [Deployment (CI/CD)](#deployment-cicd)
- [Source File Reference](#source-file-reference)
- [Custom Markdown Syntax](#custom-markdown-syntax)
- [Key Data Flows](#key-data-flows)

---

## Overview

The docs-engine converts markdown documentation from the
[docs.anychart.com](https://github.com/AnyChart/docs.anychart.com) repository into a fully
searchable, versioned documentation website. It consists of two logical parts:

1. **Generator (backend)** - Clones the docs repo, parses markdown, validates content,
   generates HTML, stores results in PostgreSQL, and triggers notifications.
2. **Web server (frontend)** - Serves the generated HTML, handles search, version routing,
   admin panel, report pages, sitemap, and offline ZIP downloads.

The two parts communicate asynchronously via **Redis queues**. They can run together in a
single process or as separate processes.

---

## Technology Stack

| Component | Technology | Version | Notes |
|-----------|-----------|---------|-------|
| Language | Clojure | 1.9.0 | JVM-based |
| Build tool | Leiningen | - | `project.clj` |
| Component system | Stuart Sierra Component | 0.3.2 | Lifecycle management |
| Web server | http-kit | 2.3.0 | Async HTTP server |
| Routing | Compojure | 1.6.1 | Ring-compatible |
| Middleware | Ring | 1.7.0 | JSON, params, keyword params |
| Templating | Selmer | 1.12.2 | Django-style templates |
| HTML generation | Hiccup | 1.0.5 | Clojure DSL for HTML |
| HTML parsing | Jsoup | 1.11.3 | TOC generation |
| HTML scraping | Enlive | 1.1.6 | Offline doc generation |
| Markdown | markdown-clj | 1.0.4 | With custom transformers |
| Database | PostgreSQL | 9.3+ | JSONB for config/report |
| DB access | clojure.java.jdbc | 0.7.8 | With HoneySQL query builder |
| Connection pool | c3p0 | 0.3.3 | JDBC connection pooling |
| Message queue | Redis (Carmine) | 2.19.1 | Task queuing |
| Full-text search | Sphinx | - | External search engine |
| Samples parser | playground-samples-parser | 0.2.6 | AnyChart proprietary |
| Link checker | link-checker | 0.2.10 | AnyChart proprietary |
| Image generation | PhantomJS + imgscalr | - | Screenshot generation |
| Version comparison | version-clj | 0.1.2 | Semantic version sorting |
| Parallel processing | Claypoole | 1.1.4 | Thread pool for page gen |
| CSS generation | Garden | 1.3.6 | Admin panel styles |
| Config format | TOML | 0.1.3 | Configuration files |
| Logging | Timbre | 4.10.0 | Structured logging |
| CI/CD | Travis CI | - | Branch-based deploy |

---

## System Architecture

The application uses Stuart Sierra's [Component](https://github.com/stuartsierra/component)
library for dependency injection and lifecycle management.

### Component Dependency Graph

```
                    +-----------+
                    | Notifier  |
                    +-----+-----+
                          |
              +-----------+-----------+
              |                       |
        +-----v-----+          +-----v-----+
        | Generator  |          |    Web    |
        +-----+-----+          +-----+-----+
              |                       |
    +---------+---------+    +--------+--------+
    |         |         |    |        |        |
+---v---+ +--v--+ +----v+  +v----+ +-v--+ +--v----+
| JDBC  | |Redis| |Offline|| JDBC| |Redis| |Sphinx |
+-------+ +-----+ |Gen   |+-----+ +-----+ +-------+
                   +------+
                      |
                +-----+-----+
                | JDBC|Redis|
                +-----+-----+
```

### Component Descriptions

| Component | Namespace | Role |
|-----------|-----------|------|
| `jdbc` | `wiki.components.jdbc` | PostgreSQL connection pool |
| `redis` | `wiki.components.redis` | Redis connection + worker creation |
| `notifier` | `wiki.components.notifier` | Slack/Skype notification dispatch |
| `generator` | `wiki.components.generator` | Redis worker that triggers doc generation |
| `offline-generator` | `wiki.components.offline-generator` | Redis worker for ZIP generation |
| `web` | `wiki.components.web` | http-kit web server |
| `sphinx` | `wiki.components.sphinx` | Sphinx full-text search connection |
| `indexer` | `wiki.components.indexer` | Redis worker that triggers search reindexing |

---

## Operating Modes

Entry point: `wiki.core/-main`

```
java -jar docs-engine.jar <mode> <config-path>
```

| Mode | System Created | Components Started | Use Case |
|------|---------------|-------------------|----------|
| `all` | `all-system` | Web + Generator + Offline-generator + Indexer + Sphinx + Notifier | Single-process deployment |
| `frontend` | `frontend-system` | Web + Indexer + Sphinx + Notifier (no generator/offline) | Serving docs only |
| `backend` | `generator-system` | Generator + Offline-generator + Notifier (no web) | Building docs only |

**Source**: `src/wiki/core.clj:110-119`

The `all` mode is used for development. In production, frontend and backend can be
separated to scale independently.

---

## Configuration

Configuration is loaded from a **TOML file** passed as the second CLI argument.

### Configuration Sections

All sections are **required** (validated via `clojure.spec` in `wiki.config.spec`):

#### `[common]` - Global settings

| Key | Type | Description |
|-----|------|-------------|
| `prefix` | string | Environment identifier: `"local"`, `"stg"`, or `"prod"` |
| `domain` | string | Base URL (e.g., `"https://docs.anychart.com/"`) |
| `reference` | string | API reference hostname (e.g., `"api.anychart.com"`) |
| `playground` | string | Playground hostname (e.g., `"playground.anychart.com"`) |
| `playground-project` | string | Default playground project name |

#### `[jdbc]` - PostgreSQL

| Key | Type | Description |
|-----|------|-------------|
| `subprotocol` | string | `"postgresql"` |
| `subname` | string | `"//localhost:5432/docs_db"` |
| `classname` | string | `"org.postgresql.Driver"` |
| `user` | string | Database user |
| `password` | string | Database password |

#### `[redis]`

| Key | Type | Description |
|-----|------|-------------|
| `host` | string | Redis hostname |
| `port` | int | Redis port |
| `db` | int | Redis database number |

#### `[generator]` - Build settings

| Key | Type | Description |
|-----|------|-------------|
| `queue` | string | Redis queue name for generation jobs |
| `redirects-queue` | string | Redis queue name for redirect updates |
| `indexer-queue` | string | Redis queue name for search reindexing |
| `git-ssh` | string | Path to SSH key wrapper script for git |
| `data-dir` | string | Working directory for cloned repos and builds |
| `static-dir` | string | Directory for static assets (images) |
| `images-dir` | string | Directory for generated screenshot images |
| `reference-versions` | string | URL to fetch API reference version list |
| `reference-default-version` | string | Fallback API version |
| `max-processes` | int | Max parallel processes |
| `show-branches` | boolean | Whether to show non-release branches |
| `generate-images` | boolean | Whether to generate PhantomJS screenshots |
| `phantom-engine` | string | Path to PhantomJS binary |
| `generator` | string | Generator identifier |

#### `[web]` - HTTP server

| Key | Type | Description |
|-----|------|-------------|
| `port` | int | HTTP listen port |
| `debug` | boolean | Debug mode |
| `queue` | string | Redis queue for triggering builds from admin |
| `redirects-queue` | string | Redis queue for redirect updates |
| `zip-queue` | string | Redis queue for ZIP generation requests |
| `static` | int | Static file cache duration |
| `max-line` | int | Max line length for content |

#### `[offline-generator]`

| Key | Type | Description |
|-----|------|-------------|
| `zip-dir` | string | Temporary directory for ZIP file assembly |
| `queue` | string | Redis queue for ZIP jobs |

#### `[sphinx]` - Full-text search

| Key | Type | Description |
|-----|------|-------------|
| `subprotocol` | string | `"mysql"` (Sphinx uses MySQL protocol) |
| `subname` | string | Sphinx connection string |
| `table` | string | Sphinx index name (e.g., `"docs_stg_index"`) |

#### `[notifications.slack]`

| Key | Type | Description |
|-----|------|-------------|
| `channel` | string | Default Slack channel |
| `token` | string | Slack webhook token |
| `username` | string | Bot username |

#### `[notifications.skype]`

| Key | Type | Description |
|-----|------|-------------|
| `id` | string | Skype bot ID |
| `chat-id` | string | Skype chat ID |
| `key` | string | Skype API key |
| `release-chat-id` | string | (optional) Separate chat for releases |

**Source**: `src/wiki/config/spec.clj`, `src/wiki/config/core.clj`

---

## Generation Pipeline

The generation pipeline is orchestrated by `wiki.generator.core/generate` and
`wiki.generator.core/generate-version`.

### Pipeline Steps

```
1. Git Fetch & Checkout
       |
2. Branch Filtering (skip unchanged commits)
       |
3. Parse Directory Structure
       |
4. Create Version Record in DB
       |
5. Copy Static Assets (images)
       |
6. Generate Pages (parallel)
   |-- Parse markdown with custom transformers
   |-- Validate links (canonical, HTTPS, direct, env)
   |-- Detect missing/broken samples
   |-- Validate image markdown syntax
   |-- Generate HTML with TOC
   |-- Store page in DB with metadata
   |-- Collect per-page error reports
       |
7. Generate Landing Page
       |
8. Remove Previous Version Data
       |
9. Trigger Offline ZIP Generation (async)
       |
10. Check Broken Links (conditional, async)
    |-- Fetch sitemap URLs
    |-- Crawl all pages for 404s
    |-- Deliver results via promise
       |
11. Store Report in DB
       |
12. Send Completion Notifications
```

### Step Details

#### 1. Git Operations (`wiki.generator.git`)

- `update-repo`: Runs `git fetch -p -P` to get latest refs
- `remote-branches`: Lists all remote branches and tags with commit hashes
- `checkout`: Copies repo to version-specific directory and checks out branch
- `merge-conflicts`: Checks for conflicts between branch and `develop`

#### 2. Branch Filtering (`wiki.generator.versions`)

- Compares current commit hash against stored version in DB
- Skips rebuilding if commit hasn't changed (`need-rebuild?`)
- Filters branches based on `show-branches` config flag

#### 3. Structure Parsing (`wiki.generator.struct`)

- Recursively walks the directory tree
- Identifies `.md` files (excluding `readme.md`)
- Reads document headers (Clojure map syntax `{:index 5 :title "Custom Title"}`)
- Builds a hierarchical tree sorted by index and title
- Reads `group.cfg` files for folder-level configuration

#### 4-5. Version & Asset Management

- Creates version record in `versions` table with tree structure as JSON
- Copies `images/` directories to static serving directory

#### 6. Page Generation (`wiki.generator.documents`)

Uses **Claypoole** thread pool (`CPU count + 2` threads) for parallel processing.

For each page:

1. **Link analysis** (`wiki.generator.analysis.core/check-links`):
   Scans raw markdown for problematic link patterns before HTML conversion.

2. **Markdown-to-HTML** (`wiki.generator.markdown/to-html`):
   Uses `markdown-clj` with custom transformer chain:
   - `branch-name-transformer`: Replaces `{branch_name}` with version
   - `image-checker`: Validates image markdown syntax
   - Standard markdown transformers
   - `sample-transformer`: Processes `{sample}...{sample}` blocks

3. **API/Playground link processing**:
   - `{api:path}text{api}` → links to API reference
   - `{pg:project/path}text{pg}` → links to playground

4. **TOC generation** (`wiki.generator.toc`):
   Parses the generated HTML with Jsoup, extracts `h1`-`h6` elements,
   builds nested `<ul>/<li>` navigation. Validates heading hierarchy.

5. **Tag extraction**: Processes `{tags}tag1, tag2{tags}` blocks.

6. **Variable replacement**: Substitutes `{{variable}}` with values from version config.

7. **Database storage**: Inserts page with URL, title, HTML content, last-modified timestamp,
   tags array, and config JSONB (containing links metadata).

#### 9. Offline ZIP (`wiki.components.offline-generator`)

Runs as a separate Redis worker. Uses futures for non-blocking generation.
See [Offline ZIP Generation](#offline-zip-generation).

#### 10. Link Checking (`wiki.generator.analysis.core/check-broken-links`)

Uses the `link-checker` library to crawl all pages from the sitemap:
- Fetches sitemap URLs from `{domain}sitemap/{version}`
- Configurable URL filtering (`check-fn`, `add-fn`)
- Excludes known false positives (LinkedIn, export-server.jar, download page)
- Results delivered via a Clojure **promise** to avoid blocking
- Max 45 crawl iterations

**Source**: `src/wiki/generator/core.clj`, `src/wiki/generator/documents.clj`

---

## Validation & Report System

This is the engine's QA system. It runs during every build and produces a report accessible
via the web interface.

### Error Detection Categories

#### Detected During Page Generation (per-page)

| Error Key | Detected By | What It Catches |
|-----------|------------|-----------------|
| `http-links` | `analysis.core/check-https` | URLs starting with `http:` (should be `https:`) |
| `non-canonical-links` | `analysis.core/check-canonical` | Links containing version numbers like `/7.13.0/` or `/latest` |
| `direct-links` | `analysis.core/check-direct` | Direct versioned links to `docs.anychart.com`, `api.anychart.com`, or `playground.anychart.com` |
| `env-links` | `analysis.core/check-env` | Links containing `stg`, `develop`, `localhost`, or `8080` |
| `sample-not-available` | `markdown/build-sample-div` | `{sample}path{sample}` references a sample that doesn't exist in the parsed samples |
| `sample-parsing-error` | `markdown/sample-transformer` | Malformed `{sample}...{sample}` syntax that throws an exception |
| `image-format-error` | `markdown/image-checker` | Image markdown with invalid parameter syntax |
| `toc-error` | `toc/add-toc`, `toc/menu-from-hs` | Missing `<h1>`, non-sequential headings (e.g., `h1` → `h3` skipping `h2`) |

#### Detected After Page Generation (version-level)

| Error Key | Detected By | What It Catches |
|-----------|------------|-----------------|
| `broken-links` | `analysis.core/check-broken-links` | Actual HTTP 404 errors found by crawling generated pages |

#### Link Analysis Details

The `check-links` function in `analysis.core` scans raw markdown (before HTML conversion)
using regex to extract all `[text](url)` patterns. It then runs each URL through four
checker functions:

```
check-canonical: Rejects URLs containing /X.Y.Z/ patterns or /latest
check-https:     Rejects URLs starting with http:
check-direct:    Rejects versioned URLs pointing to anychart domains
check-env:       Rejects URLs containing stg/develop/localhost/8080
```

The version-level config can specify `report.http-ignore` — a list of URL substrings
to exempt from the HTTPS check.

### Report Data Structure

Stored as JSONB in `versions.report`:

```clojure
{:error-links
 [{:page-url "Quick_Start/Getting_Started"
   :http-links ["http://example.com/resource"]
   :non-canonical-links ["/7.13.0/api/anychart"]
   :direct-links ["https://docs.anychart.com/7.13.0/Quick_Start"]
   :env-links ["https://stg.anychart.com/something"]
   :sample-not-available ["Quick_Start/Getting_Started"]
   :sample-parsing-error ["Quick_Start/Getting_Started"]
   :image-format-error ["Quick_Start/Getting_Started"]
   :toc-error ["h3 - nested too deep"]}
  ;; ... one entry per page with errors
  ]

 :broken-links
 [{:url "https://docs.anychart.com/develop/Quick_Start/Getting_Started"
   :links [{:href "https://broken.example.com" :text "link text"}]}
  ;; ... one entry per page with 404s
  ]

 :check-broken-links-disabled false}  ;; true if link checker was skipped
```

### Accessing Reports

#### HTML Report Page

**URL**: `GET /:version/report`

Rendered by `wiki.generator.analysis.page/page` using Hiccup. Shows:

- "Report for version **X.Y.Z**" heading with link to JSON version
- **"Pages with errors"** section: Table per page listing all error types and values.
  Shows green "Not found." if no errors.
- **"Pages with 404 links"** section: Table with page URL and broken links.
  Shows gray message if link checker was disabled, with instructions to use
  `#links` or `#all` commit message flags.

**Source**: `src/wiki/generator/analysis/page.clj:17-86`

#### JSON Report API

**URL**: `GET /:version/report.json`

Returns the raw report JSONB with `Access-Control-Allow-Origin: *` header.

**Source**: `src/wiki/web/handlers/report_handlers.clj:7-12`

#### Admin Panel Access

The admin panel at `/_admin_` has a **"Show report"** button that opens the report page
for the currently selected version.

### When Link Checking Runs

Controlled by `wiki.generator.core/need-check-links`:

```
Link checking is ENABLED when:
  - Branch is a released version (matches v\d+ pattern)
  - Branch is "master"
  - Commit message contains "#links"
  - Commit message contains "#all"
  - Rebuild triggered with "with link checking" from admin

Link checking is DISABLED when:
  - prefix is "local" (development)
  - Fast mode rebuild from admin
  - None of the above conditions met
```

When disabled, the report's `check-broken-links-disabled` flag is set to `true` and the
report page shows a gray notice explaining how to enable it.

**Source**: `src/wiki/generator/core.clj:67-76`

---

## Notification System

Notifications are dispatched through `wiki.components.notifier` which delegates to
`wiki.notification.slack` and `wiki.notification.skype`.

### Notification Events

| Event | Channels | Trigger |
|-------|----------|---------|
| Build started | Slack | New generation cycle begins |
| Version build started | Slack, Skype | Individual version build begins |
| Version build complete | Slack, Skype | Individual version finishes (with error summary) |
| Build complete | Slack | All versions finished |
| Build failed | Slack, Skype | Exception during generation |
| Sample not available | Slack (delayed) | `{sample}` references missing sample |
| Sample parsing error | Slack (delayed) | `{sample}` syntax error |
| Image format error | Slack (delayed) | Image markdown syntax error |
| 404 detected | Slack (`#docs-404-errors`) | Visitor hits a 404 page |

### Completion Summary Message

When a version build completes, the notifier aggregates all errors into a summary:

```
Direct links: N
Non canonical links: N
Http links: N
Env links: N
Unavailable samples: N
Parsing samples errors: N
Parsing images errors: N
TOC errors: N
Conflicts with develop: N
404 errors: N
```

Only non-zero counts are included. If all counts are zero, a simple "complete" message
is sent instead of a warnings summary.

### Slack Implementation Details

- Uses Slack incoming webhooks (`anychart-team.slack.com/services/hooks/incoming-webhook`)
- Per-page errors (sample, image) use `send` on a Clojure `agent` for batching
  (`notify-attach-delay`) — they accumulate and are sent together with the next
  non-delayed notification
- Build lifecycle messages use `notify-attach` which flushes the agent queue
- 404 errors go to a dedicated `#docs-404-errors` channel

**Source**: `src/wiki/notification/slack.clj`, `src/wiki/components/notifier.clj`

---

## Web Application & Routing

The web layer is defined in `wiki.web.routes` using Compojure.

### Route Map

| Method | Path | Handler | Description |
|--------|------|---------|-------------|
| GET | `/` | `show-landing` | Redirects to Quick_Start |
| GET | `/_admin_` | `admin-handlers/admin-page` | Admin panel |
| GET | `/_redirects_` | `admin-handlers/redirects-page` | Show active redirects |
| GET | `/_update_` | `admin-handlers/update-versions` | Trigger build (webhook) |
| POST | `/_update_` | `admin-handlers/update-versions` | Trigger build (webhook) |
| POST | `/_delete_` | `admin-handlers/delete-version` | Delete a version |
| POST | `/_rebuild_` | `admin-handlers/rebuild-version` | Rebuild specific version |
| GET | `/sitemap` | `sitemap-handlers/show-sitemap` | Sitemap index |
| GET | `/sitemap.xml` | `sitemap-handlers/show-sitemap` | Sitemap XML |
| GET | `/sitemap/:version` | `sitemap-handlers/show-sitemap-version` | Per-version sitemap |
| GET | `/latest` | `show-latest` | Redirect to latest Quick_Start |
| GET | `/latest/*` | `try-show-latest-page` | Redirect /latest/X to /X |
| POST | `/links` | `links-handlers/links` | Links API |
| GET | `/:version/check/*` | `try-show-page` | Check if page exists |
| GET | `/:version/search` | `search-page` | Search within version |
| POST | `/:version/search-data` | `search-data` | Search API (JSON) |
| GET | `/:version/_generate-zip_` | `zip-handlers/generate-zip` | Trigger ZIP generation |
| GET | `/:version/download` | `zip-handlers/download-zip` | Download offline ZIP |
| **GET** | **`/:version/report.json`** | **`report-handlers/report`** | **JSON report** |
| **GET** | **`/:version/report`** | **`report-handlers/report-page`** | **HTML report page** |
| GET | `/*-json` | `show-page-data` | Page data as JSON |
| GET | `/*` | `show-page-middleware` | Display documentation page |

### Middleware Stack

Applied in order (bottom-up in code, outermost first):

1. `wrap-json-response` — JSON response encoding
2. `wrap-google-analytics-optimize` — Detect GA Speed Insights user agent
3. `wrap-redirect` — Apply configured URL redirects
4. `wrap-json-params` — Parse JSON request bodies
5. `wrap-params` — Parse query string parameters
6. `wrap-keyword-params` — Keywordize parameter names

### Version Resolution

The `check-version-middleware` extracts the version from the URL path, looks up matching
versions in the database, and passes the resolved version to the handler. If no matching
version is found, it returns 404.

The `check-version-middleware-by-url` handles the ambiguity where the first path segment
could be either a version key or a page URL.

**Source**: `src/wiki/web/routes.clj`

---

## Admin Panel

**URL**: `/_admin_`

The admin panel provides version management with these controls:

### Controls

| Control | Action |
|---------|--------|
| **Start updating versions** | Enqueues a `generate` command — equivalent to GitHub webhook |
| **Version selector** | Dropdown of all non-hidden versions, sorted by semantic version |
| **Rebuild (dropdown)** | Three rebuild modes for selected version |
| **Remove** | Deletes selected version from database |
| **Show report** | Opens `/:version/report` for selected version |
| **Show redirects** | Opens `/_redirects_` to view active URL redirects |

### Rebuild Modes

| Mode | Parameter | Link Checking |
|------|-----------|---------------|
| Commit message flags | `{}` | Based on commit message `#links`/`#all` flags |
| Fast | `{:fast true}` | Disabled |
| With link checking | `{:linkchecker true}` | Enabled |

When rebuilding, the existing version data is first removed from the database, then a
generate job is enqueued to Redis with the version key and mode parameters.

**Source**: `src/wiki/views/admin/admin_page.clj`, `src/wiki/web/handlers/admin_handlers.clj`

---

## Database Schema

Defined in `src/sql/scheme.sql`.

### `versions` Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER (PK) | Auto-increment via `version_id_seq` |
| `key` | VARCHAR(255) | Version identifier (e.g., `"8.0.0"`, `"develop"`) |
| `commit` | VARCHAR(40) | Git commit hash |
| `hidden` | BOOLEAN | Soft-delete flag (default `false`) |
| `tree` | TEXT | JSON string of directory tree structure |
| `zip` | BYTEA | Binary offline documentation ZIP file |
| `config` | JSONB | Version configuration from `config.toml` in docs repo |
| `report` | JSONB | Validation report (see [Report Data Structure](#report-data-structure)) |

### `folders` Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER (PK) | Auto-increment via `folder_id_seq` |
| `version_id` | INTEGER (FK) | References `versions.id` |
| `url` | VARCHAR(255) | Folder URL path |
| `default_page` | VARCHAR(255) | Default page name when folder URL is accessed |

### `pages` Table

| Column | Type | Description |
|--------|------|-------------|
| `id` | INTEGER (PK) | Auto-increment via `page_id_seq` |
| `version_id` | INTEGER (FK) | References `versions.id` |
| `url` | VARCHAR(255) | Page URL path |
| `full_name` | VARCHAR(255) | Display title |
| `content` | TEXT | Generated HTML content |
| `last_modified` | BIGINT | Unix timestamp of last git commit to source file |
| `tags` | VARCHAR(255)[] | PostgreSQL array of tag strings |
| `config` | JSONB | Page metadata including links (api, playground, samples) |

### Entity Relationships

```
versions 1──────N pages
versions 1──────N folders
```

When a version is deleted or rebuilt, all associated pages and folders are cascade-deleted
in application code (not via DB constraints).

---

## Offline ZIP Generation

The system generates downloadable ZIP archives of the documentation for offline use.

### Flow

1. After page generation completes, `generate-zip` is called on the `offline-generator`
   component
2. The component uses an atom-backed state map with futures to prevent duplicate generation
3. `wiki.offline.core/generate-zip` creates self-contained HTML files:
   - Downloads and embeds external resources (CSS, JS, images)
   - Rewrites links for local file access
   - Uses `local-page.selmer` template (different from online template)
4. The resulting ZIP is stored as BYTEA in the `versions.zip` column
5. Users download via `GET /:version/download`

### Trigger Points

- Automatically after every successful version build
- Manually via `GET /:version/_generate-zip_` from admin

**Source**: `src/wiki/components/offline_generator.clj`, `src/wiki/offline/core.clj`,
`src/wiki/offline/zip.clj`

---

## Staging vs Production

### Environment Detection

The `common.prefix` config value determines the environment:

| Prefix | Environment | Domain |
|--------|-------------|--------|
| `"local"` | Development | `localhost:PORT` |
| `"stg"` | Staging | `docs.anychart.stg` |
| `"prod"` | Production | `docs.anychart.com` |

### Behavioral Differences

| Feature | Local | Staging | Production |
|---------|-------|---------|------------|
| Link checking | Always disabled | Conditional (commit flags) | Always enabled for releases |
| Branch visibility | Configurable | Configurable | Releases + master only |
| Notifications | Active | Active | Active |
| Report generation | Active | Active | Active |
| Sphinx search index | `docs_local_index` | `docs_stg_index` | `docs_prod_index` |

### Staging-Specific Workflow

1. Push to `staging` branch → Travis CI builds and deploys to `docs-stg` servers
2. All branches/versions are rebuilt from git
3. Reports are generated at `/:version/report`
4. Review the report for errors before promoting to production
5. Broken link checking can be triggered with `#links` in commit message
6. Merge `staging` → `master` to deploy to production

---

## Deployment (CI/CD)

### Travis CI Pipeline (`.travis.yml`)

```
Build: lein uberjar
  → Produces: target/uberjar/wiki-2.0-standalone.jar

Branch Mapping:
  master  → APP=docs-prod
  staging → APP=docs-stg
  bible   → APP=bible-stg

Deploy Steps:
  1. Decrypt SSH key (.travis/id_rsa.enc)
  2. SCP JAR to Server 1 (104.236.66.244:/apps/$APP/)
  3. SCP JAR to Server 2 (68.183.148.118:/apps/$APP/)
  4. SSH restart: supervisorctl restart $APP
```

### Server Setup

- Servers run the JAR under **supervisord**
- Each app (`docs-prod`, `docs-stg`, `bible-stg`) has its own:
  - Config TOML file
  - PostgreSQL database
  - Redis instance/database number
  - Sphinx index

---

## Source File Reference

### Core

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/core.clj` | `wiki.core` | Entry point, system definitions, `-main` |
| `src/wiki/config/core.clj` | `wiki.config.core` | Runtime config accessor functions |
| `src/wiki/config/spec.clj` | `wiki.config.spec` | Configuration validation specs |

### Components

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/components/generator.clj` | `wiki.components.generator` | Redis worker for doc generation |
| `src/wiki/components/offline_generator.clj` | `wiki.components.offline-generator` | Redis worker for ZIP generation |
| `src/wiki/components/web.clj` | `wiki.components.web` | http-kit server component |
| `src/wiki/components/jdbc.clj` | `wiki.components.jdbc` | PostgreSQL connection component |
| `src/wiki/components/redis.clj` | `wiki.components.redis` | Redis connection + worker management |
| `src/wiki/components/sphinx.clj` | `wiki.components.sphinx` | Sphinx search component |
| `src/wiki/components/indexer.clj` | `wiki.components.indexer` | Search index updater |
| `src/wiki/components/notifier.clj` | `wiki.components.notifier` | Notification dispatch |

### Generator

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/generator/core.clj` | `wiki.generator.core` | Main generation orchestration |
| `src/wiki/generator/documents.clj` | `wiki.generator.documents` | Per-page generation with parallel processing |
| `src/wiki/generator/markdown.clj` | `wiki.generator.markdown` | Markdown→HTML with custom transformers |
| `src/wiki/generator/toc.clj` | `wiki.generator.toc` | Table of contents generation |
| `src/wiki/generator/git.clj` | `wiki.generator.git` | Git operations (fetch, checkout, conflicts) |
| `src/wiki/generator/struct.clj` | `wiki.generator.struct` | Directory structure parsing |
| `src/wiki/generator/tree.clj` | `wiki.generator.tree` | Navigation tree generation |
| `src/wiki/generator/versions.clj` | `wiki.generator.versions` | Branch filtering, version cleanup |
| `src/wiki/generator/api_versions.clj` | `wiki.generator.api-versions` | API reference version fetching |
| `src/wiki/generator/analysis/core.clj` | `wiki.generator.analysis.core` | Link validation + broken link crawling |
| `src/wiki/generator/analysis/page.clj` | `wiki.generator.analysis.page` | HTML report page rendering |
| `src/wiki/generator/phantom/core.clj` | `wiki.generator.phantom.core` | PhantomJS screenshot generation |
| `src/wiki/generator/phantom/download.clj` | `wiki.generator.phantom.download` | Resource downloading for phantomJS |

### Data Access

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/data/versions.clj` | `wiki.data.versions` | Version CRUD, report storage, ZIP access |
| `src/wiki/data/pages.clj` | `wiki.data.pages` | Page CRUD, search |
| `src/wiki/data/folders.clj` | `wiki.data.folders` | Folder CRUD |
| `src/wiki/data/search.clj` | `wiki.data.search` | Sphinx search queries |
| `src/wiki/data/utils.clj` | `wiki.data.utils` | JSONB conversion utilities |

### Web

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/web/routes.clj` | `wiki.web.routes` | All route definitions and middleware |
| `src/wiki/web/helpers.clj` | `wiki.web.helpers` | Request helper functions (jdbc, redis, config) |
| `src/wiki/web/redirects.clj` | `wiki.web.redirects` | URL redirect middleware and config parsing |
| `src/wiki/web/tree.clj` | `wiki.web.tree` | Navigation tree rendering (Selmer tag) |
| `src/wiki/web/handlers/admin_handlers.clj` | `wiki.web.handlers.admin-handlers` | Admin panel routes |
| `src/wiki/web/handlers/report_handlers.clj` | `wiki.web.handlers.report-handlers` | Report JSON and HTML endpoints |
| `src/wiki/web/handlers/search_handlers.clj` | `wiki.web.handlers.search-handlers` | Search page and API |
| `src/wiki/web/handlers/sitemap_handlers.clj` | `wiki.web.handlers.sitemap-handlers` | Sitemap generation |
| `src/wiki/web/handlers/zip_handlers.clj` | `wiki.web.handlers.zip-handlers` | ZIP download and generation trigger |
| `src/wiki/web/handlers/links_handlers.clj` | `wiki.web.handlers.links-handlers` | Links API |
| `src/wiki/web/handlers/handers_404.clj` | `wiki.web.handlers.handers-404` | 404 error handler (note: typo in filename) |

### Views

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/views/admin/admin_page.clj` | `wiki.views.admin.admin-page` | Admin panel HTML (Hiccup) |
| `src/wiki/views/main/main_page.clj` | `wiki.views.main.main-page` | Main page HTML |
| `src/wiki/views/common.clj` | `wiki.views.common` | Shared HTML components |
| `src/wiki/views/iframe.clj` | `wiki.views.iframe` | Sample iframe HTML |
| `src/wiki/views/page404/page404.clj` | `wiki.views.page404.page404` | 404 page HTML |
| `src/wiki/views/resources.clj` | `wiki.views.resources` | Resource URL helpers |

### Notifications

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/notification/slack.clj` | `wiki.notification.slack` | Slack webhook integration |
| `src/wiki/notification/skype.clj` | `wiki.notification.skype` | Skype bot integration |

### Offline

| File | Namespace | Purpose |
|------|-----------|---------|
| `src/wiki/offline/core.clj` | `wiki.offline.core` | Offline HTML generation |
| `src/wiki/offline/zip.clj` | `wiki.offline.zip` | ZIP file creation |

### Templates (Selmer)

| File | Purpose |
|------|---------|
| `resources/templates/page.selmer` | Main documentation page template |
| `resources/templates/local-page.selmer` | Offline/local page template |
| `resources/templates/doc_sample.selmer` | Embedded sample with iframe |
| `resources/templates/sample.selmer` | Interactive sample with playground link |
| `resources/templates/samples-update.selmer` | Sample initialization script |
| `resources/templates/phantom.selmer` | PhantomJS rendering template |
| `resources/templates/search.selmer` | Search page template |
| `resources/templates/search-results.html` | Search results fragment |
| `resources/templates/404.selmer` | Error page with search |
| `resources/templates/google-analytics.selmer` | GA tracking code |
| `resources/templates/page.html` | Legacy sample page |

### SQL

| File | Purpose |
|------|---------|
| `src/sql/scheme.sql` | Database schema (versions, folders, pages) |
| `src/sql/search.sql` | Sphinx full-text search query |

---

## Custom Markdown Syntax

The docs engine extends standard markdown with these custom syntax elements:

### Samples

```markdown
{sample}Sample_Path/Sample_Name{sample}
{sample :width 500 :height 300}Sample_Path/Sample_Name{sample}
{sample :width "100%" :height "400px"}Sample_Path/Sample_Name{sample}
```

Embeds an interactive sample from the playground as an iframe.
The sample path is looked up in the parsed samples from the docs repo's `samples/` directory.

### API Reference Links

```markdown
{api:anychart.charts.Cartesian#column}column(){api}
```

Generates a link to the API reference: `//api.anychart.com/{version}/anychart.charts.Cartesian#column`

### Playground Links

```markdown
{pg:project/path/to/sample}View in Playground{pg}
```

Generates a link to the playground: `//playground.anychart.com/{project}/{version}/{path}`

### Tags

```markdown
{tags}tag1, tag2, tag3{tags}
```

Adds tags to the page (rendered as `<span>` elements after the `<h1>`).
Must appear in the source markdown; stripped from rendered output.

### Branch Name

```markdown
{branch_name}
```

Replaced with the current version/branch name during generation.

### Variables

```markdown
{{variable_name}}
```

Replaced with values from the version config's `[vars]` section.

### Document Headers

At the very beginning of a markdown file, an optional Clojure map configures the page:

```clojure
{:index 5 :title "Custom Page Title"}
```

- `:index` — Sort order within parent folder (default: 1000)
- `:title` — Override the auto-generated title (filename-based)

### Folder Configuration

A `group.cfg` file in any folder can configure it:

```clojure
{:index 3 :title "Getting Started"}
```

---

## Key Data Flows

### Build Trigger Flow

```
GitHub push → webhook → GET /_update_
  → admin-handlers/update-versions
  → Redis enqueue {cmd: "generate"}
  → Generator worker picks up message
  → generator/generate-docs
  → generator.core/generate
  → ... (see Generation Pipeline)
```

### Admin Rebuild Flow

```
Admin panel → POST /_rebuild_ {version: "8.0.0", fast: true}
  → admin-handlers/rebuild-version
  → Remove existing version from DB
  → Redis enqueue {cmd: "generate", version: "8.0.0", fast: true}
  → Generator worker picks up message
  → Rebuilds only that version
```

### Page Request Flow

```
GET /8.0.0/Quick_Start
  → wrap-redirect (check for URL redirects)
  → check-version-middleware-by-url
    → Resolve "8.0.0" as version key
    → Fetch version from DB
    → Fetch page "Quick_Start" from DB
  → show-page-middleware
    → Load page HTML, tree, versions list
    → Render page.selmer template
  → Response
```

### Report Access Flow

```
GET /8.0.0/report
  → report-handlers/report-page
  → vdata/version-report (query versions.report JSONB)
  → analysis-page/page (render Hiccup HTML)
  → Response (full HTML report page)
```

---

## Known Issues & Notes

1. **Filename typo**: `src/wiki/web/handlers/handers_404.clj` — missing 'l' in "handlers"
2. **Skype notifications partially disabled**: Most Skype calls are commented out in
   `notifier.clj`, only error/warning notifications remain active
3. **PhantomJS**: Deprecated technology, only used for screenshot generation which is
   optional (`generate-images` config flag)
4. **MySQL dependency**: Present in `project.clj` solely for Sphinx (which uses MySQL
   wire protocol), not for data storage
5. **Clojure 1.9**: Relatively old version; upgrading would enable newer spec features
6. **No Docker**: Deployment uses direct JAR deployment via SCP, no containerization
7. **Travis CI**: Travis CI has changed its free tier; may need migration to GitHub Actions
   or similar
