[<img src="https://cdn.anychart.com/images/logo-transparent-segoe.png?2" width="234px" alt="AnyChart - Robust JavaScript/HTML5 Chart library for any project">](https://anychart.com)

# AnyChart Docs Engine

[AnyChart Documentation](https://docs.anychart.com/) is a service with all the reference information that users might
need while working with AnyChart products. The application consists of two parts: the web part and the generator.
The Web part is just a site responsible for displaying the content, and the generator is a parser whose main task is
to parse the [corresponding repository](https://github.com/AnyChart/docs.anychart.com).
Each article there is written using Markdown format. Both application parts communicate via Redis queues.
The application backend is written on Clojure and the frontend on Javascript with jQuery.

[![Build Status](https://travis-ci.com/AnyChart/docs-engine.svg?token=ERMLfyrvWdA8g6gi11Vp&branch=master)](https://travis-ci.com/AnyChart/docs-engine)
[Production](http://docs.anychart.com)

[![Build Status](https://travis-ci.com/AnyChart/docs-engine.svg?token=ERMLfyrvWdA8g6gi11Vp&branch=staging)](https://travis-ci.com/AnyChart/docs-engine)
[Staging](http://docs.anychart.stg)

## Documentation

See **[ARCHITECTURE.md](ARCHITECTURE.md)** for complete technical documentation, including:

- [System architecture & component graph](ARCHITECTURE.md#system-architecture)
- [Operating modes](ARCHITECTURE.md#operating-modes) (`all`, `frontend`, `backend`)
- [Full configuration reference](ARCHITECTURE.md#configuration) (all TOML sections)
- [Generation pipeline](ARCHITECTURE.md#generation-pipeline) (step-by-step)
- [Validation & report system](ARCHITECTURE.md#validation--report-system) — missing samples, broken links, TOC errors, etc.
- [Web routes & middleware](ARCHITECTURE.md#web-application--routing)
- [Admin panel](ARCHITECTURE.md#admin-panel)
- [Database schema](ARCHITECTURE.md#database-schema)
- [Staging vs production differences](ARCHITECTURE.md#staging-vs-production)
- [Deployment (CI/CD)](ARCHITECTURE.md#deployment-cicd)
- [Custom markdown syntax](ARCHITECTURE.md#custom-markdown-syntax) (`{sample}`, `{api}`, `{pg}`, etc.)
- [Complete source file reference](ARCHITECTURE.md#source-file-reference)

## Quick Start

### Install dependencies

```
sudo apt-get install phantomjs
sudo apt-get install redis-server
sudo apt-get install sphinxsearch
sudo mkdir -p /var/data/sphinx/
sudo npm install grunt-cli -g
```

### Setup local database

The application uses PostgreSQL, so you need to create database and user:
```sql
CREATE USER docs_user WITH PASSWORD 'pass';
CREATE DATABASE docs_db;
GRANT ALL PRIVILEGES ON DATABASE docs_db TO docs_user;
psql -p5432 -d docs_db -U docs_user -W
```

Then run the schema from `src/sql/scheme.sql`.

### Build & Run

```bash
# Build the JAR
lein uberjar

# Run in all-in-one mode
java -jar target/uberjar/wiki-2.0-standalone.jar all config.toml

# Run frontend only (serve existing docs)
java -jar target/uberjar/wiki-2.0-standalone.jar frontend config.toml

# Run backend only (generate docs)
java -jar target/uberjar/wiki-2.0-standalone.jar backend config.toml
```

### Staging Reports

After a build completes, validation reports are available at:
- **HTML**: `http://<host>/<version>/report` — visual report with error tables
- **JSON**: `http://<host>/<version>/report.json` — machine-readable report data

Reports check for: missing samples, broken links (404s), non-HTTPS links, non-canonical
versioned links, environment-specific URLs, image syntax errors, and TOC structure problems.

See [ARCHITECTURE.md - Validation & Report System](ARCHITECTURE.md#validation--report-system) for details.

## License

If you have any questions regarding licensing - please contact us. <sales@anychart.com>
