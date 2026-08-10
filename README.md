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

2. **Configure environment variables**:
   Create or modify `src/main/resources/application.properties` with your database, Redis, and Cloudinary credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/devconnect
   spring.datasource.username=YOUR_MYSQL_USERNAME
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   
   spring.data.redis.host=YOUR_REDIS_HOST
   spring.data.redis.port=YOUR_REDIS_PORT
   spring.data.redis.password=YOUR_REDIS_PASSWORD
   ```

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
