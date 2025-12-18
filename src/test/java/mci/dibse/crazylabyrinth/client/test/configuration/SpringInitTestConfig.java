package mci.dibse.crazylabyrinth.client.test.configuration;

import static mci.dibse.crazylabyrinth.client.configuration.SpringBootConfiguration.BASE_PACKAGE;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;

@TestConfiguration
@ComponentScan(BASE_PACKAGE)
@Import(BaseTestConfiguration.class)
@PropertySource("classpath:application.properties")
public class SpringInitTestConfig {}
