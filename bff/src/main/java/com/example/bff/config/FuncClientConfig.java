package com.example.bff.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class FuncClientConfig {

  @Bean(name = "productFunc")
  public WebClient productFunc(@Value("${func.productBaseUrl}") String base) {
    return WebClient.builder().baseUrl(base).build();
  }

  @Bean(name = "warehouseFunc")
  public WebClient warehouseFunc(@Value("${func.warehouseBaseUrl}") String base) {
    return WebClient.builder().baseUrl(base).build();
  }
}
