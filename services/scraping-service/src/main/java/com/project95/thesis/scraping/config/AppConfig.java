package com.project95.thesis.scraping.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(ClientProperties.class)
public class AppConfig {

  private SimpleClientHttpRequestFactory createRequestFactory(ClientProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getTimeouts().getConnectMs());
    requestFactory.setReadTimeout(properties.getTimeouts().getReadMs());
    return requestFactory;
  }

  @Bean
  @Primary
  public RestClient thesisServiceClient(RestClient.Builder builder, ClientProperties properties) {
    return builder
        .baseUrl(properties.getMainThesis().getUrl())
        .requestFactory(createRequestFactory(properties))
        .build();
  }

  @Bean
  public RestClient genAiServiceClient(RestClient.Builder builder, ClientProperties properties) {
    return builder
        .baseUrl(properties.getGenAi().getUrl())
        .requestFactory(createRequestFactory(properties))
        .build();
  }

  @Bean
  public RestClient scrapingClient(RestClient.Builder builder, ClientProperties properties) {
    return builder.requestFactory(createRequestFactory(properties)).build();
  }
}
