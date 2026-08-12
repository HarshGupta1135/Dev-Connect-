# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Stage 1 — builder: full JDK and Maven, needed only to produce the jar.
#
# Java 25, matching <java.version> in pom.xml. A 17 image cannot run these class
# files: the JVM refuses anything compiled for a newer release than itself, so
# the container would die on startup with UnsupportedClassVersionError.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25-alpine AS builder

WORKDIR /build

# The pom lands in its own layer so the dependency download is only repeated when
# dependencies actually change. Copying the source first would invalidate the
# cache on every edit and refetch the whole tree.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src

# Tests are skipped on purpose. This suite is @SpringBootTest against MySQL,
# Redis and Gmail SMTP, none of which exist inside an image build — it would fail
# on a missing datasource, not on your code. Run ./mvnw test in CI, where those
# services can be started alongside it.
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Stage 2 — runtime: JRE only. None of Maven, the JDK compiler, the dependency
# cache or the source survives into this stage, which is what keeps the image
# small.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:25-jre-alpine AS runtime

WORKDIR /app

# Nothing in the app needs root, so it does not run as root.
RUN addgroup -S spring && adduser -S spring -G spring

# One jar in target/ matches this: spring-boot-maven-plugin's repackaged artifact.
# The pre-repackage copy is left as .jar.original, so the glob skips it.
#
# Ownership is set by COPY rather than a following RUN chown: a layer records whole
# files, not permission deltas, so chowning an 80MB jar would write a second 80MB
# copy of it into the image.
COPY --chown=spring:spring --from=builder /build/target/*.jar app.jar

USER spring

EXPOSE 8080

# /health is permitAll in SpringSecurity, so this needs no credentials. wget is
# busybox's, already present in alpine — no extra package to install.
#
# ${PORT:-8080} rather than a literal: a platform that assigns the port at runtime sets
# PORT, and the app follows it via server.port, so a hardcoded probe would report a
# healthy container as unhealthy.
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- "http://localhost:${PORT:-8080}/health" || exit 1

# Exec form, so the JVM is PID 1 and receives SIGTERM directly on `docker stop`
# and shuts down gracefully instead of being killed after the grace period.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
