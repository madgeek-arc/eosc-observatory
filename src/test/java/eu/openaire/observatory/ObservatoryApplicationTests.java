package eu.openaire.observatory;

import gr.uoa.di.madgik.authorization.config.AuthorizationAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

@SpringBootTest(classes = AuthorizationAutoConfiguration.class, properties = "spring.profiles.active=test")
@ImportTestcontainers(IntegrationTestConfig.class)
class ObservatoryApplicationTests {

    @Test
    void contextLoads() {
    }

}
