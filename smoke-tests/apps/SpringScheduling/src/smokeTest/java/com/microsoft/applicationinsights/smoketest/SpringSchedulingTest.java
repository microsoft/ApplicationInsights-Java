// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_23_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class SpringSchedulingTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/should-ignore")
  void shouldIgnoreTest() throws Exception {
    // sleep a bit to make sure no dependencies are reported
    Thread.sleep(5000);
    assertThat(testing.mockedIngestion.getCountForType("RemoteDependencyData")).isZero();
  }

  @Test
  @TargetUri("/scheduler")
  void fixedRateSchedulerTest() throws Exception {

    // wait for the http request generated by this test
    testing.mockedIngestion.waitForItem(
        new Predicate<Envelope>() {
          @Override
          public boolean test(Envelope input) {
            if (!"RequestData".equals(input.getData().getBaseType())) {
              return false;
            }
            RequestData data = (RequestData) ((Data<?>) input.getData()).getBaseData();
            return data.getName().equals("GET /SpringScheduling/scheduler");
          }
        },
        10,
        TimeUnit.SECONDS);

    // wait for at least two spring scheduler "requests"
    testing.mockedIngestion.waitForItems(
        new Predicate<Envelope>() {
          @Override
          public boolean test(Envelope input) {
            if (!"RequestData".equals(input.getData().getBaseType())) {
              return false;
            }
            RequestData data = (RequestData) ((Data<?>) input.getData()).getBaseData();
            return data.getName().equals("SpringSchedulingApp.fixedRateScheduler");
          }
        },
        2,
        10,
        TimeUnit.SECONDS);

    List<Envelope> exceptionEnvelopes =
        testing.mockedIngestion.getItemsEnvelopeDataType("ExceptionData");

    assertThat(exceptionEnvelopes)
        .anySatisfy(
            envelope -> {
              ExceptionData ed = (ExceptionData) ((Data<?>) envelope.getData()).getBaseData();
              List<ExceptionDetails> details = ed.getExceptions();
              ExceptionDetails ex = details.get(0);
              assertThat(ex.getTypeName()).isEqualTo("java.lang.RuntimeException");
              assertThat(ex.getMessage()).isEqualTo("exceptional");
              assertThat(ed.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
              assertThat(ed.getProperties())
                  .containsEntry("Logger Message", "Unexpected error occurred in scheduled task");
              assertThat(ed.getProperties()).containsEntry("SourceType", "Logger");
              assertThat(ed.getProperties())
                  .containsEntry(
                      "LoggerName",
                      "org.springframework.scheduling.support.TaskUtils$LoggingErrorHandler");
              assertThat(ed.getProperties()).containsKey("ThreadName");
              assertThat(ed.getProperties()).hasSize(4);
            });

    assertThat(exceptionEnvelopes)
        .anySatisfy(
            envelope -> {
              ExceptionData ed = (ExceptionData) ((Data<?>) envelope.getData()).getBaseData();
              List<ExceptionDetails> details = ed.getExceptions();
              ExceptionDetails ex = details.get(0);
              assertThat(ex.getTypeName()).isEqualTo("java.lang.RuntimeException");
              assertThat(ex.getMessage()).isEqualTo("exceptional");
              assertThat(ed.getSeverityLevel()).isNull();
              assertThat(ed.getProperties()).isEmpty();
            });
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_17_OPENJ9)
  static class Tomcat8Java17OpenJ9Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_21_OPENJ9)
  static class Tomcat8Java21OpenJ9Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_23)
  static class Tomcat8Java23Test extends SpringSchedulingTest {}

  @Environment(TOMCAT_8_JAVA_23_OPENJ9)
  static class Tomcat8Java23OpenJ9Test extends SpringSchedulingTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends SpringSchedulingTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends SpringSchedulingTest {}
}
