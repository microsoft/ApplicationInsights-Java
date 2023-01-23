// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SpringBootApp extends SpringBootServletInitializer {

  public static void main(String[] args) {
    ApplicationInsights.attach();
    SpringApplication.run(SpringBootApp.class, args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
    return applicationBuilder.sources(SpringBootApp.class);
  }
}
