package com.mariem.assurance;

import com.mariem.assurance.config.TestSecurityConfig;  // Import de ta config test
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;


@SpringBootTest
@Import(TestSecurityConfig.class)  // <--- Ajout ici pour fournir le JwtDecoder mock
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AssuranceNetworkApiApplicationTests {

	@Test
	void contextLoads() {
		// Test que le contexte se charge correctement
	}
}
