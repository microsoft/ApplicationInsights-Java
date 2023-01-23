// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class SpringBootApp extends SpringBootServletInitializer {

  private static final String FAKE_INGESTION_ENDPOINT = "http://host.testcontainers.internal:6060/";

  public static void main(String[] args) {
    ApplicationInsights.attach();
    ConnectionString.configure(
        "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;IngestionEndpoint="
            + FAKE_INGESTION_ENDPOINT
            + ";LiveEndpoint="
            + FAKE_INGESTION_ENDPOINT);
    SpringApplication.run(SpringBootApp.class, args);
  }

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {
    return applicationBuilder.sources(SpringBootApp.class);
  }
}
