/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.smoketest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.runners.Parameterized;

public abstract class AiWarSmokeTest extends AiSmokeTest {

  private static final String PREFIX =
      "ghcr.io/open-telemetry/opentelemetry-java-instrumentation/smoke-test-servlet";
  private static final String SUFFIX = "20211216.1584506476";

  private static final String TOMCAT_APP_DIR = "/server/webapps";
  private static final String WILDFLY_APP_DIR = "/opt/jboss/wildfly/standalone/deployments";

  @Parameterized.Parameters(name = "{index}: {0} {1}")
  public static List<Object[]> parameterGenerator() {
    if (USE_MATRIX) {
      return Arrays.asList(
          // TODO (trask) add Java 18
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk8-" + SUFFIX, TOMCAT_APP_DIR},
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk8-openj9-" + SUFFIX, TOMCAT_APP_DIR},
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk11-" + SUFFIX, TOMCAT_APP_DIR},
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk11-openj9-" + SUFFIX, TOMCAT_APP_DIR},
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk17-" + SUFFIX, TOMCAT_APP_DIR},
          new Object[] {PREFIX + "-wildfly:13.0.0.Final-jdk8-" + SUFFIX, WILDFLY_APP_DIR});
    } else {
      return Collections.singletonList(
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk8-" + SUFFIX, TOMCAT_APP_DIR});
    }
  }

  public static List<Object[]> parameterGeneratorJava8() {
    if (USE_MATRIX) {
      return Arrays.asList(
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk8-" + SUFFIX, TOMCAT_APP_DIR},
          new Object[] {PREFIX + "-wildfly:13.0.0.Final-jdk8-" + SUFFIX, WILDFLY_APP_DIR});
    } else {
      return Collections.singletonList(
          new Object[] {PREFIX + "-tomcat:8.5.72-jdk8-" + SUFFIX, TOMCAT_APP_DIR});
    }
  }
}
