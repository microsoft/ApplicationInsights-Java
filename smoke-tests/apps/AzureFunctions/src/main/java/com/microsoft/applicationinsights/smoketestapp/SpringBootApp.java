// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.azure.functions.worker.handler.FunctionEnvironmentReloadRequestHandler;
import java.lang.reflect.Field;
import java.util.Map;
import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.microsoft.applicationinsights.smoketestapp")
public class SpringBootApp {

  private static final String FAKE_BREEZE_INGESTION_ENDPOINT =
      "http://host.testcontainers.internal:6060/";

  public static void main(String[] args) throws Exception {
    // Set up Azure Functions environment
    setEnv("AzureWebJobsStorage", "dummy");
    setEnv(
        "APPLICATIONINSIGHTS_CONNECTION_STRING",
        "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;IngestionEndpoint="
            + FAKE_BREEZE_INGESTION_ENDPOINT
            + ";LiveEndpoint="
            + FAKE_BREEZE_INGESTION_ENDPOINT);

    new FunctionEnvironmentReloadRequestHandler().execute();

    // Start embedded Tomcat with Spring MVC
    Tomcat tomcat = new Tomcat();
    tomcat.setPort(8080);
    tomcat.getConnector();
    Context context = tomcat.addContext("", System.getProperty("java.io.tmpdir"));

    AnnotationConfigWebApplicationContext appContext = new AnnotationConfigWebApplicationContext();
    appContext.setServletContext(context.getServletContext());
    appContext.register(SpringBootApp.class);
    appContext.refresh();

    DispatcherServlet dispatcher = new DispatcherServlet(appContext);
    Tomcat.addServlet(context, "dispatcher", dispatcher).setLoadOnStartup(1);
    context.addServletMappingDecoded("/*", "dispatcher");

    tomcat.start();
    tomcat.getServer().await();
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
