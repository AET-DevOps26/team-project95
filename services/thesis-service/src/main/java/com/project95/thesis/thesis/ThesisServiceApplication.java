package com.project95.thesis.thesis;

import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class ThesisServiceApplication {
  private static final String SOURCE_REGISTRY_COMMAND = "--sync-source-registry";

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(ThesisServiceApplication.class);

    boolean sourceRegistryCommand = hasSourceRegistryCommand(args);
    if (sourceRegistryCommand) {
      application.setWebApplicationType(WebApplicationType.NONE);
    }

    ConfigurableApplicationContext context = application.run(args);
    if (sourceRegistryCommand) {
      System.exit(SpringApplication.exit(context));
    }
  }

  private static boolean hasSourceRegistryCommand(String[] args) {
    return Arrays.asList(args).contains(SOURCE_REGISTRY_COMMAND);
  }
}
