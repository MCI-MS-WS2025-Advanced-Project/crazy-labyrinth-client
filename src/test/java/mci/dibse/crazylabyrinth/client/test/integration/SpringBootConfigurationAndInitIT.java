package mci.dibse.crazylabyrinth.client.test.integration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javafx.application.Application;
import mci.dibse.crazylabyrinth.client.JavaFXClientApplication;
import mci.dibse.crazylabyrinth.client.configuration.SpringBootConfiguration;
import mci.dibse.crazylabyrinth.client.test.configuration.SpringInitTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SpringJUnitConfig(SpringInitTestConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SpringBootConfigurationAndInitIT {

    @Value("${spring.application.name}")
    private String applicationName;

    @Autowired
    private Environment environment;

    @Autowired
    private Application javaFXClientApplication;

    @Test
    void environmetPropsAreVisible() {
        assertNotNull(environment.getProperty("spring.application.name"));
    }

   @Test
    void propertiesAreVisible() {
        assertNotNull(applicationName);
    }

    @Test
    public void test() throws Exception {
        setFullModePreservingFormatting("full");
        doReturn(new Application.Parameters() {

                     @Override
                     public List<String> getRaw() {
                         return List.of();
                     }
                     @Override
                     public List<String> getUnnamed() {
                         return List.of();
                     }
                     @Override
                     public Map<String, String> getNamed() {
                         return Map.of();
                     }
                 }
        ).when(javaFXClientApplication).getParameters();

        assertThatNoException().isThrownBy( () ->
        SpringBootConfiguration.initSpringBasedOnProperties((JavaFXClientApplication) javaFXClientApplication)
        );
    }

    @Test
    public void test2() throws Exception {
        setFullModePreservingFormatting("minimal");
        assertThatNoException().isThrownBy( () ->
                SpringBootConfiguration.initSpringBasedOnProperties((JavaFXClientApplication) javaFXClientApplication)
        );
    }


      void setFullModePreservingFormatting(String springMode) throws Exception {
        Path props = Paths.get("src/test/resources/application.properties");
        setOrAppendPropertyLine(props, "app.spring.mode", springMode);
      }

      void setOrAppendPropertyLine(Path props, String key, String value) throws Exception {
          List<String> lines = Files.exists(props)
                  ? Files.readAllLines(props, StandardCharsets.UTF_8)
                  : new ArrayList<>();

          for (int i = 0; i < lines.size(); i++) {
              String line = lines.get(i);
              if (line.strip().startsWith("#") || line.strip().isEmpty()) continue;
              if (line.startsWith(key + "=") || line.startsWith(key + " =")) {
                  lines.set(i, key + "=" + value);
                  break;
              }
          }
          Files.write(props, lines, StandardCharsets.UTF_8);
      }
}
