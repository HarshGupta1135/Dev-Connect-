# DevConnect - High-Performance Developer Job Board & Matching Platform

DevConnect is a modern, high-performance job board designed to match developers with suitable job postings using skill proximity scoring. It is built on a scalable Java Spring Boot backend featuring an optimized Redis caching layer, eager object fetching, automatic cron scheduling, and document storage.

---

## 🚀 Key Features

* **Intelligent Skill-based Matching**: Features a Jaccard-similarity proximity search that ranks jobs based on matched skills rather than simple text lookups.
* **Optimized Redis Caching**: Implements a high-efficiency caching strategy via Jackson JSON serialization, resolving standard Spring Page serialization limitations and achieving a **14.54x speedup** on repeated searches.
* **Automatic Expiration Scheduler**: Executes a daily midnight cron job to close expired listings and automatically evict cached indices to prevent stale results.
* **Secure Document Uploads**: Integrates with Cloudinary to handle multipart developer resume uploads safely.
* **Swagger/OpenAPI Documentation**: Automatically generated interactive UI displaying and documenting all endpoints.

---

## 🛠️ Technology Stack

* **Core Framework**: Java 25 & Spring Boot 4
* **Database**: MySQL (Primary Database)
* **Caching**: Redis (Upstash)
* **Security**: JSON Web Token (JWT) & Spring Security
* **Storage Service**: Cloudinary
* **Documentation**: Springdoc OpenAPI v3

---

## 📖 API Documentation & Swagger UI

Once the application is running locally, you can view the fully interactive API documentation, test endpoints, and inspect payloads via the **Swagger UI**:

👉 **Swagger URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)

---

## ⚙️ Getting Started

### 📋 Prerequisites
* JDK 25 or higher
* Maven 3.9+
* Running MySQL instance
* Running Redis server / Upstash connection details

### 📦 Setup Instructions

1. **Clone the repository**:
   ```bash
   git clone https://github.com/your-username/DevConnect.git
   cd DevConnect
   ```

2. **Configure credentials**:
   `application.yaml` holds no secrets. Every credential is read from a `.env` file in the
   project root, which is git-ignored. Copy the template and fill it in:
   ```bash
   cp .env.example .env
   ```
   ```properties
   DB_PASSWORD=your-mysql-password
   MAIL_USERNAME=your.address@gmail.com
   MAIL_PASSWORD=your-gmail-app-password
   REDIS_HOST=your-redis-host
   REDIS_PASSWORD=your-redis-password
   CLOUDINARY_CLOUD_NAME=your-cloud-name
   CLOUDINARY_API_KEY=your-api-key
   CLOUDINARY_API_SECRET=your-api-secret
   JWT_SECRET=a-random-string-of-at-least-32-characters
   ```
   In a deployed environment, set these as real environment variables instead — they take
   precedence over the file, and no `.env` is needed.

   If a value is missing from both, the app refuses to start rather than falling back to a
   default. Note that the error may be a downstream one: a missing `DB_PASSWORD` surfaces as
   `Access denied for user 'root'`, because Spring leaves an unresolved placeholder as literal
   text when binding configuration properties. A missing `JWT_SECRET` reports itself directly
   as `Could not resolve placeholder 'jwt.secret'`. If startup fails on credentials, check
   `.env` against `.env.example` first.

3. **Run Maven tests**:
   Ensure all tests compile and pass successfully:
   ```bash
   ./mvnw clean test
   ```

4. **Start the application**:
   ```bash
   ./mvnw spring-boot:run
   ```

---

## 🖥️ Frontend

The React client lives in [`devconnect-frontend/`](devconnect-frontend) (Vite + React 19).
Run it in a second terminal, with the backend already up on 8080:

```bash
cd devconnect-frontend
npm install
npm run dev          # http://localhost:3000
```

The Vite dev server proxies `/api`, `/admin` and `/health` to `http://localhost:8080`,
so the browser only ever talks to one origin and the backend needs no CORS configuration
during development. For a deployed build, set `VITE_API_BASE_URL` to the backend origin
instead — see `devconnect-frontend/.env.example` — at which point the backend does have
to allow that origin.

On Windows use `mvnw.cmd` in place of `./mvnw` throughout.

---

## 🐳 Running with Docker

```bash
docker compose up --build      # backend + its own MySQL + its own Redis
docker compose down            # stop, keeping the data
docker compose down -v         # stop and delete the databases
```

The backend is published on 8080 as usual. MySQL and Redis are published on **3307**
and **6380** rather than their defaults, so the stack does not collide with a MySQL or
Redis already running on the host; the app itself reaches them over the compose
network by service name, not through those mappings.

`.env` still supplies the real secrets — mail, Cloudinary, `JWT_SECRET`. What
`docker-compose.yml` overrides is anything describing *where* a service lives, since
`DB_URL` names `localhost` (which inside a container is the container) and `REDIS_HOST`
names Upstash. The throwaway MySQL and Redis get their own passwords, defaulted in the
compose file so the stack never borrows the host database's password or the Upstash
token; set `LOCAL_DB_PASSWORD` / `LOCAL_REDIS_PASSWORD` to change them.

### Recommended day-to-day setup

Run the databases in Docker and the application however suits the task. Both reach the
same data, so nothing has to be migrated when you switch:

```bash
docker compose up -d mysql redis     # leave running; the data lives in a volume
./mvnw spring-boot:run               # development: ~8s start, devtools reload, debugger
docker compose up -d app             # before deploying: exercises the real image
```

Only one of the last two at a time — both want port 8080. `.env` needs no editing
between them, because `docker-compose.yml` overrides the addresses for the container.

Two consequences worth being deliberate about:

* A MySQL installed on the host is now a *second, divergent* copy of the data. Stop its
  service so nothing reaches it by accident.
* `docker compose down -v` deletes the volume, and there is no folder left behind to
  recover from. Dump before anything schema-related:

  ```bash
  docker exec devconnect-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --hex-blob --databases devconnect' > backups/devconnect-$(date +%F).sql
  ```

### Schema migrations

Flyway owns the schema; `ddl-auto` is `validate`, so Hibernate checks the mapping and
changes nothing. Migrations are plain SQL in `src/main/resources/db/migration`, applied
in version order at startup and recorded in `flyway_schema_history`.

To change the schema, add a file — never edit one that has already run, since Flyway
checksums them and will refuse to start on a mismatch:

```
V2__add_something.sql
```

`baseline-on-migrate` is on, so a database that already has the V1 tables is recorded as
being at V1 and the script is skipped; an empty one gets the script instead. This is
what `ddl-auto: update` could not do: it silently declined to add the unique constraints
on `users.user_name` and `users.email` to a table that already existed, so they had to be
applied by hand and existed in no file.

### Seeding the compose database from your local one

The stack keeps its data in a Docker volume, entirely separate from a MySQL installed
on the host — so it starts empty, with only the admin user `DataInitializer` creates.
To copy an existing local database across:

```bash
# 1. dump the host database
mysqldump -uroot -p --single-transaction --hex-blob --databases devconnect > devconnect-host.sql

# 2. start MySQL alone and let it become healthy, before the app can create a schema
docker compose up -d mysql

# 3. load the dump
docker exec -i devconnect-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < devconnect-host.sql

# 4. now start the rest
docker compose up -d
```

Keep the dump out of version control — it contains real user rows.

The MySQL image is pinned to `8.0` to match a typical local install. This matters in
both directions: a data directory initialised by 8.4 cannot be opened by 8.0, and a
dump taken from 8.0 should not be restored into a server a major version ahead of it.

---

## ☁️ Deploying free: Render + Aiven + Upstash

Nothing in the code changes for this — Aiven is real MySQL, so the driver, dialect and
migrations are the same ones used locally.

| Piece | Where | Free plan reality |
| --- | --- | --- |
| Spring Boot app | Render web service | 750 instance-hours/month; spins down after 15 min idle, ~1 min to wake |
| MySQL | Aiven | 1 GB RAM, 1 GB storage, no expiry; powers off after prolonged inactivity |
| Redis | Upstash | already provisioned |

Render has **no managed MySQL**, and its free PostgreSQL is **deleted 30 days after
creation** — which is why the database is Aiven's rather than Render's own.

### 1. Aiven MySQL

Create a free MySQL service, then take its host, port, user and password from the
service overview. The port is not 3306, the user is `avnadmin`, and the database is
`defaultdb` — a free plan cannot add others.

Aiven requires TLS, so the URL needs `sslMode=REQUIRED`; without it the connection is
refused rather than downgraded:

```
DB_URL=jdbc:mysql://<host>:<port>/defaultdb?sslMode=REQUIRED
DB_USERNAME=avnadmin
DB_PASSWORD=<from the Aiven console>
```

### 2. Render

New → **Blueprint** → this repo. `render.yaml` declares the service, pins the Dockerfile
builder, and health-checks `/health`, so a failed migration fails the deploy instead of
going live broken. Then set the environment variables it asks for — mail, Cloudinary,
`JWT_SECRET` and the three `DB_` values above.

Leave `PORT` alone: Render assigns it and `server.port` follows it.

On first boot Flyway finds an empty database and applies `V1`, so the schema builds
itself. Watch for `Migrating schema "defaultdb" to version "1"` in the logs.

### What the free tier costs you in practice

The app sleeps after 15 minutes, so the first visitor after a quiet spell waits about a
minute for the JVM to start. Aiven also powers off an idle database, which has to be
resumed from its console. Neither loses data — both are slow to wake, which is the
trade for paying nothing.

---

## ☁️ Deploying to Railway (paid)

Railway has managed MySQL and Redis, which makes it a single-dashboard alternative — but
not a free one. Its Free plan carries $1/month of usage credit while this stack costs
roughly $9–10/month to keep running, so expect Hobby ($5/month) plus overage.

Add a **MySQL** service, a **Redis** service and a service pointed at this repo. The
Dockerfile is detected automatically, and `railway.toml` pins that plus the health check
at `/health`, so a failed migration fails the deploy instead of going live broken.

### Variables

Railway's own `DATABASE_URL` / `REDIS_URL` **cannot be used directly.** They are
`mysql://user:pass@host:port/db` and `redis://default:pass@host:port`, while this app
needs a JDBC URL and Redis split into host, port and password. Reference the individual
variables instead — the `${{Service.VAR}}` syntax is Railway's, resolved at deploy time:

| Variable | Value |
| --- | --- |
| `DB_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}` |
| `DB_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `DB_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `REDIS_HOST` | `${{Redis.REDISHOST}}` |
| `REDIS_PORT` | `${{Redis.REDISPORT}}` |
| `REDIS_PASSWORD` | `${{Redis.REDISPASSWORD}}` |
| `REDIS_SSL` | `false` — TLS is an Upstash requirement, not Railway's internal network |
| `JWT_SECRET` | a fresh random string, 32+ characters |
| `MAIL_USERNAME`, `MAIL_PASSWORD` | Gmail address and app password |
| `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` | from Cloudinary |
| `CORS_ALLOWED_ORIGINS` | the deployed client's origin, once it exists |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | optional; seeds one admin on first boot |

Do not set `PORT` — Railway assigns it and `server.port` follows it.

`ADMIN_PASSWORD` is what creates the administrator, and nothing is seeded without it.
Set it to something you generate, log in once, then remove the variable.

### First deploy

Flyway finds an empty database and applies `V1`, so the schema builds itself with no
manual step. Watch for `Migrating schema "devconnect" to version "1"` in the logs.

To carry local data across, dump it and pipe it into Railway's MySQL over its **public**
proxy — `MYSQL_PUBLIC_URL`, not the internal host, which is only reachable from inside
their network:

```bash
docker exec devconnect-mysql sh -c 'exec mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --hex-blob devconnect' > dump.sql
mysql -h <proxy-host> -P <proxy-port> -u root -p railway < dump.sql
```

Load it *before* the app's first boot, or Flyway will have created the schema and the
dump's `CREATE TABLE` statements will collide.

---

## 🧪 Testing Verification

The test suite validates the caching performance and automatic scheduling. A sample execution yields the following logs:
```text
First call (Cache Miss - Database): 407 ms
ACTUAL KEYS IN REDIS: [job-listings::0-10----]
Successfully verified key '0-10----' is present in Redis!
Second call (Cache Hit - Redis): 28 ms
Caching speedup ratio: 14.54x faster!
```
