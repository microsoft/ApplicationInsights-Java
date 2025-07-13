// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_21;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class ExceptionMessageHandlingTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/testExceptionWithoutMessage")
  void testExceptionWithoutMessage() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);

    Envelope edEnvelope = edList.get(0);
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    // Verify that exceptions without messages have their class name as the message
    // This prevents the 206 error: "Field 'message' on type 'ExceptionDetails' is required but
    // missing or empty"
    assertThat(ed.getExceptions().get(0).getTypeName()).isEqualTo("java.lang.NullPointerException");
    assertThat(ed.getExceptions().get(0).getMessage()).isEqualTo("java.lang.NullPointerException");
    assertThat(ed.getExceptions().get(0).getMessage()).isNotEmpty();
    assertThat(ed.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
  }

  @Test
  @TargetUri("/testExceptionWithEmptyMessage")
  void testExceptionWithEmptyMessage() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);

    Envelope edEnvelope = edList.get(0);
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    // Verify that exceptions with empty messages have their class name as the message
    assertThat(ed.getExceptions().get(0).getTypeName()).isEqualTo("java.lang.RuntimeException");
    assertThat(ed.getExceptions().get(0).getMessage()).isEqualTo("java.lang.RuntimeException");
    assertThat(ed.getExceptions().get(0).getMessage()).isNotEmpty();
    assertThat(ed.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
  }

  @Test
  @TargetUri("/testExceptionWithWhitespaceMessage")
  void testExceptionWithWhitespaceMessage() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);

    Envelope edEnvelope = edList.get(0);
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    // Verify that exceptions with whitespace-only messages have their class name as the message
    assertThat(ed.getExceptions().get(0).getTypeName())
        .isEqualTo("java.lang.IllegalArgumentException");
    assertThat(ed.getExceptions().get(0).getMessage())
        .isEqualTo("java.lang.IllegalArgumentException");
    assertThat(ed.getExceptions().get(0).getMessage()).isNotEmpty();
    assertThat(ed.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends ExceptionMessageHandlingTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends ExceptionMessageHandlingTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends ExceptionMessageHandlingTest {}

  @Environment(TOMCAT_8_JAVA_21)
  static class Tomcat8Java21Test extends ExceptionMessageHandlingTest {}
}
