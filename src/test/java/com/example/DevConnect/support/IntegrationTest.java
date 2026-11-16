package com.example.DevConnect.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A test that starts the whole application against a real MySQL and Redis.
 *
 * Skipped by plain `mvn test`. Locally: `docker compose up -d mysql redis`, then
 * `mvn verify -Dtest.excludedGroups=manual`. CI starts both as service containers.
 *
 * It uses whatever database the app is configured for (.env locally), so tests
 * must roll back (@Transactional) or clean up after themselves, and must never be
 * pointed at the production database.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@Tag("integration")
public @interface IntegrationTest {
}
