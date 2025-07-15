// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("disabled_applicationinsights.json")
class LogbackDisabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/test")
  void testDisabled() throws Exception {
    testing.waitAndAssertTrace(
        trace -> {
          trace.hasRequestSatisying(request -> request.hasName("GET /Logback/test"));

          // Check that no messages are logged
          trace.hasMessageCount(0);
        });

    // Also check the count directly
    assertThat(testing.mockedIngestion.getCountForType("MessageData")).isZero();
  }

  @Test
  @TargetUri("/testWithSpanException")
  void testWithSpanException() throws Exception {
    testing.waitAndAssertTrace(
        trace -> {
          trace.hasRequestSatisying(
              request -> request.hasName("GET /Logback/testWithSpanException"));

          // Check that no messages are logged
          trace.hasMessageCount(0);

          // Check that span exception is still captured
          assertThat(trace.getExceptions()).hasSize(1);

          Envelope edEnvelope = trace.getExceptions().get(0);
          new ExceptionAssert(edEnvelope)
              .hasExceptionType("java.lang.RuntimeException")
              .hasExceptionMessage("Test Exception")
              .hasEmptyProperties(); // this is not a logger-based exception
        });

    // Also check the count directly
    assertThat(testing.mockedIngestion.getCountForType("MessageData")).isZero();

    // Assert parent-child relationship separately using the original method
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edList.get(0), "GET /Logback/testWithSpanException");
  }
}
