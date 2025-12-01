// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.azure.functions.worker.handler.FunctionEnvironmentReloadRequestHandler;
import java.lang.reflect.Field;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBootApp {

  private static final String FAKE_BREEZE_INGESTION_ENDPOINT =
      "http://host.testcontainers.internal:6060/";

  public static void main(String[] args) throws Exception {

    setEnv("AzureWebJobsStorage", "dummy");
    setEnv(
        "APPLICATIONINSIGHTS_CONNECTION_STRING",
        "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;IngestionEndpoint="
            + FAKE_BREEZE_INGESTION_ENDPOINT
            + ";LiveEndpoint="
            + FAKE_BREEZE_INGESTION_ENDPOINT);

    new FunctionEnvironmentReloadRequestHandler().execute();

    SpringApplication.run(SpringBootApp.class, args);
  }

  public static void setEnv(String name, String value) throws Exception {
    Map<String, String> env = System.getenv();
    Class<?> cl = env.getClass();
    Field field = cl.getDeclaredField("m");
    field.setAccessible(true);
    @SuppressWarnings("unchecked") // safe unchecked cast - type verified by runtime context
    Map<String, String> writableEnv = (Map<String, String>) field.get(env);
    writableEnv.put(name, value);
  }
}
