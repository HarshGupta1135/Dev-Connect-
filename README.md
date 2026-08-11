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

### Seeding the compose database from your local one

The stack keeps its data in a Docker volume, entirely separate from a MySQL installed
on the host — so it starts empty, with only the admin user `DataInitializer` creates.
To copy an existing local database across:

```bash
# 1. dump the host database (--hex-blob matters: users.role is a serialised blob)
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

## 🧪 Testing Verification

The test suite validates the caching performance and automatic scheduling. A sample execution yields the following logs:
```text
First call (Cache Miss - Database): 407 ms
ACTUAL KEYS IN REDIS: [job-listings::0-10----]
Successfully verified key '0-10----' is present in Redis!
Second call (Cache Hit - Redis): 28 ms
Caching speedup ratio: 14.54x faster!
```
