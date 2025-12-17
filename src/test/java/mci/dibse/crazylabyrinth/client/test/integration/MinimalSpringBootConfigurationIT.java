package mci.dibse.crazylabyrinth.client.test.integration;

import static org.junit.jupiter.api.Assertions.*;

import mci.dibse.crazylabyrinth.client.test.configuration.MinimalTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
/**
 *  @author Julian Janetschek
 */
@SpringJUnitConfig(MinimalTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MinimalSpringBootConfigurationIT {

    @Value("${spring.application.name}")
    String applicationName;

    @Autowired Environment environment;

    @Test
    void environmetPropsAreVisible() {
        assertNotNull(environment.getProperty("spring.application.name"));
    }

    @Test
    void propertiesAreVisible() {
        assertNotNull(applicationName);
    }
}
