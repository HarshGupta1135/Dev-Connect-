package com.example.DevConnect;

import com.example.DevConnect.support.IntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Boots the full application: every bean wires, Flyway migrates, and Hibernate's
 * `validate` confirms the entities match the migrated schema.
 */
@IntegrationTest
class DevConnectApplicationTests {

	@Test
	void contextLoads() {
	}

}
