// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.attach.ApplicationInsights;
import com.microsoft.applicationinsights.connectionstring.ConnectionString;
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

  private static final String FAKE_INGESTION_ENDPOINT = "http://host.testcontainers.internal:6060/";

  public static void main(String[] args) throws Exception {
    // Attach Application Insights agent at runtime
    ApplicationInsights.attach();
    // Configure connection string after attach
    ConnectionString.configure(
        "InstrumentationKey=00000000-0000-0000-0000-0FEEDDADBEEF;IngestionEndpoint="
            + FAKE_INGESTION_ENDPOINT
            + ";LiveEndpoint="
            + FAKE_INGESTION_ENDPOINT);

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
}
