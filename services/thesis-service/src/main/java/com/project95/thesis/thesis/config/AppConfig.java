package com.project95.thesis.thesis.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ClientProperties.class)
public class AppConfig {

  @Bean
  public RestClient restClient(RestClient.Builder builder, ClientProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    
    requestFactory.setConnectTimeout(properties.getTimeouts().getConnectMs());
    requestFactory.setReadTimeout(properties.getTimeouts().getReadMs());
    
    return builder
      .baseUrl(properties.getVectorSearch().getUrl())
      .requestFactory(requestFactory)
      .build();
  }
}
