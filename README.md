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

## 🧪 Testing Verification

The test suite validates the caching performance and automatic scheduling. A sample execution yields the following logs:
```text
First call (Cache Miss - Database): 407 ms
ACTUAL KEYS IN REDIS: [job-listings::0-10----]
Successfully verified key '0-10----' is present in Redis!
Second call (Cache Hit - Redis): 28 ms
Caching speedup ratio: 14.54x faster!
```
