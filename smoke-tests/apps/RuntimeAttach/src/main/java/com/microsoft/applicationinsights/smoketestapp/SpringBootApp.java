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
    // Pre-load MethodHandle to avoid ClassCircularityError on Java 17 with runtime attach
    // See https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/3396
    try {
      Class.forName("java.lang.invoke.MethodHandle");
    } catch (ClassNotFoundException e) {
      // ignore
    }
    ApplicationInsights.attach();
    SpringApplication.run(SpringBootApp.class, args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
    return applicationBuilder.sources(SpringBootApp.class);
  }
}
