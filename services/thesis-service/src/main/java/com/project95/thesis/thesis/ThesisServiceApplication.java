package com.project95.thesis.thesis;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ThesisServiceApplication {
  private static final String SYNC_SOURCE_REGISTRY_COMMAND = "--sync-source-registry";

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(ThesisServiceApplication.class);

    boolean syncSourceRegistry = hasSyncSourceRegistryCommand(args);
    if (syncSourceRegistry) {
      application.setWebApplicationType(WebApplicationType.NONE);
    }

    ConfigurableApplicationContext context = application.run(args);
    if (syncSourceRegistry) {
      System.exit(SpringApplication.exit(context));
    }
  }

  private static boolean hasSyncSourceRegistryCommand(String[] args) {
    return Arrays.asList(args).contains(SYNC_SOURCE_REGISTRY_COMMAND);
  }
}
