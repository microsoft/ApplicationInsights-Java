// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketestapp;

import java.lang.management.ManagementFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.weakref.jmx.MBeanExporter;
import org.weakref.jmx.Managed;
import org.weakref.jmx.Nested;

@SpringBootApplication
public class SpringBootApp extends SpringBootServletInitializer {

  @Override
  protected SpringApplicationBuilder configure(SpringApplicationBuilder applicationBuilder) {

    MBeanExporter exporter = new MBeanExporter(ManagementFactory.getPlatformMBeanServer());
    exporter.export("test:name=X", new NestedExample());

    return applicationBuilder.sources(SpringBootApp.class);
  }

  public static class NestedExample {
    private final NestedObject nestedObject = new NestedObject();

    @Nested
    public NestedObject getNestedObject() {
      return nestedObject;
    }

    public static final class NestedObject {
      @Managed
      public int getValue() {
        return 5;
      }
    }
  }
}
