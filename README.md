# DevConnect - High-Performance Developer Job Board & Matching Platform

DevConnect is a modern, high-performance job board designed to match developers with suitable job postings using skill proximity scoring. It is built on a scalable Java Spring Boot backend featuring an optimized Redis caching layer, eager object fetching, automatic cron scheduling, and document storage.

---

## 🚀 Key Features

* **Intelligent Skill-based Matching**: Ranks jobs by the share of a posting's required skills that the developer actually holds, so extra unrelated skills never push a strong candidate down the list.
* **Optimized Redis Caching**: Implements a high-efficiency caching strategy via JSON serialization, resolving standard Spring Page serialization limitations and achieving a **~14x speedup** on repeated searches. The cache key covers page, size, sort order and every filter, so no request is ever served another request's page.
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
   `application.yaml` contains no secrets - every credential is read from an environment
   variable, with a git-ignored local file for development. Copy the example and fill it in:
   ```bash
   cp src/main/resources/application-local-example.yaml src/main/resources/application-local.yaml
   ```
   `application-local.yaml` is git-ignored and is loaded automatically (the default active
   profile is `local`). It needs your MySQL, Redis, Cloudinary and JWT values:
   ```yaml
   spring:
     datasource:
       password: YOUR_MYSQL_PASSWORD
     data:
       redis:
         host: YOUR_REDIS_HOST
         password: YOUR_REDIS_PASSWORD
   jwt:
     secret: A_RANDOM_STRING_OF_AT_LEAST_32_CHARACTERS
   ```
   In any other environment set the variables instead - `DB_URL`, `DB_USERNAME`,
   `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`,
   `REDIS_PASSWORD`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`,
   `CLOUDINARY_API_SECRET`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS` - and start with
   `SPRING_PROFILES_ACTIVE=prod`. Startup fails fast if a required secret is missing.

   The first admin account is not created automatically. To bootstrap one, set
   `ADMIN_BOOTSTRAP_ENABLED=true` together with `ADMIN_BOOTSTRAP_USERNAME`,
   `ADMIN_BOOTSTRAP_EMAIL` and a strong `ADMIN_BOOTSTRAP_PASSWORD`.

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

The suite mixes fast unit tests with integration tests:

* `SkillMatchUtilTest`, `JwtUtilTest`, `ApplicationServiceTest` - plain unit tests (Mockito where
  needed). No MySQL, Redis or SMTP required.
* `JobListingCacheTest`, `ApplicationUniqueConstraintTest`, `JobSchedulerServiceTest`,
  `RedisConnectionTest`, `RejectedDevelopersTest` - integration tests that need a running MySQL
  and Redis.
* `MailTest` is `@Disabled`: it delivers a real email, so it is only run manually when
  verifying SMTP credentials.

```bash
./mvnw clean test
```

Sample caching output:
```text
First call (cache miss, database): 451 ms
Second call (cache hit, Redis): 31 ms
```
