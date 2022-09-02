// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.util.concurrent.Executor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@SpringBootApplication
@EnableAsync
public class SpringBootApp extends SpringBootServletInitializer {

  public SpringBootApp() {
    super();

    // This lets tomcat handle error and hence filter catches exception.
    // Disables Springboot error handling which prevents response from propagating up.
    // See:
    // https://github.com/spring-projects/spring-boot/commit/6381a07c71310c56dc29cf99709adf5fe6e6406a
    setRegisterErrorPageFilter(false);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
    return applicationBuilder.sources(SpringBootApp.class);
  }

  public static void main(String[] args) {
    SpringApplication.run(SpringBootApp.class, args);
  }

  @Bean
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(2);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("AsyncTaskExecutor-");
    executor.initialize();
    return executor;
  }
}
