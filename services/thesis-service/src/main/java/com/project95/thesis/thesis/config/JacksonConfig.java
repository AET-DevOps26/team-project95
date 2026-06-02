package com.project95.thesis.thesis.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {
  // JsonNullableModule removed as it is incompatible with Jackson 3 in this environment.
  // DTOs are now generated with openApiNullable=false.
}
