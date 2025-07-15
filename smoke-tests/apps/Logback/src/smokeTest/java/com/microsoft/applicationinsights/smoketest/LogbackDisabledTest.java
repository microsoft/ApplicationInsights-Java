// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
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
  void testDisabled() {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request -> request.hasName("GET /Logback/test").hasSuccess(true))
                .hasMessageCount(0));
  }

  @Test
  @TargetUri("/testWithSpanException")
  void testWithSpanException() throws Exception {
    testing.waitAndAssertTrace(
        trace ->
            trace
                .hasRequestSatisying(
                    request ->
                        request.hasName("GET /Logback/testWithSpanException").hasSuccess(true))
                .hasMessageCount(0));

    // Check that span exception is still captured
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 1, operationId);

    Envelope edEnvelope = edList.get(0);
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    assertThat(ed.getExceptions().get(0).getTypeName()).isEqualTo("java.lang.RuntimeException");
    assertThat(ed.getExceptions().get(0).getMessage()).isEqualTo("Test Exception");
    assertThat(ed.getProperties()).isEmpty(); // this is not a logger-based exception

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope, "GET /Logback/testWithSpanException");
  }
}
