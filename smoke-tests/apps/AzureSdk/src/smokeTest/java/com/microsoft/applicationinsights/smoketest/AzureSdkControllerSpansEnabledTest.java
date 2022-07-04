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

import static com.microsoft.applicationinsights.smoketest.WarEnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("controller_spans_enabled_applicationinsights.json")
class AzureSdkControllerSpansEnabledTest {

  @RegisterExtension static final AiSmokeTest testing = new AiSmokeTest();

  @Test
  @TargetUri("/test")
  void test() throws Exception {
    Telemetry telemetry = testing.getTelemetry(2);

    if (!telemetry.rdd1.getName().equals("TestController.test")) {
      RemoteDependencyData rddTemp = telemetry.rdd1;
      telemetry.rdd1 = telemetry.rdd2;
      telemetry.rdd2 = rddTemp;

      Envelope rddEnvelopeTemp = telemetry.rddEnvelope1;
      telemetry.rddEnvelope1 = telemetry.rddEnvelope2;
      telemetry.rddEnvelope2 = rddEnvelopeTemp;
    }

    assertThat(telemetry.rd.getName()).isEqualTo("GET /AzureSdk/test");
    assertThat(telemetry.rd.getUrl()).matches("http://localhost:[0-9]+/AzureSdk/test");
    assertThat(telemetry.rd.getResponseCode()).isEqualTo("200");
    assertThat(telemetry.rd.getSuccess()).isTrue();
    assertThat(telemetry.rd.getSource()).isNull();
    assertThat(telemetry.rd.getProperties()).isEmpty();
    assertThat(telemetry.rd.getMeasurements()).isEmpty();

    assertThat(telemetry.rdd1.getName()).isEqualTo("TestController.test");
    assertThat(telemetry.rdd1.getData()).isNull();
    assertThat(telemetry.rdd1.getType()).isEqualTo("InProc");
    assertThat(telemetry.rdd1.getTarget()).isNull();
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    assertThat(telemetry.rdd2.getName()).isEqualTo("hello");
    assertThat(telemetry.rdd2.getData()).isNull();
    assertThat(telemetry.rdd2.getType()).isEqualTo("InProc");
    assertThat(telemetry.rdd2.getTarget()).isNull();
    assertThat(telemetry.rdd2.getProperties()).isEmpty();
    assertThat(telemetry.rdd2.getSuccess()).isTrue();

    AiSmokeTest.assertParentChild(
        telemetry.rd, telemetry.rdEnvelope, telemetry.rddEnvelope1, "GET /AzureSdk/test");
    AiSmokeTest.assertParentChild(
        telemetry.rdd1, telemetry.rddEnvelope1, telemetry.rddEnvelope2, "GET /AzureSdk/test");
  }
}
