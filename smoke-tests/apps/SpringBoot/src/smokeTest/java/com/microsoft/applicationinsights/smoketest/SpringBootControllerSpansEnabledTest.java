// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.smoketest.schemav2.RemoteDependencyData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@Environment(TOMCAT_8_JAVA_8)
@UseAgent("controller_spans_enabled_applicationinsights.json")
class SpringBootControllerSpansEnabledTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/basic/trackEvent")
  void trackEvent() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    Envelope rdEnvelope = rdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();

    String operationId = rdEnvelope.getTags().get("ai.operation.id");

    testing.mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

    // TODO get event data envelope and verify value
    List<EventData> data = testing.mockedIngestion.getTelemetryDataByTypeInRequest("EventData");

    assertThat(data).anySatisfy(ed -> assertThat(ed.getName()).isEqualTo("EventDataTest"));

    assertThat(data)
        .anySatisfy(
            ed -> {
              assertThat(ed.getName()).isEqualTo("EventDataPropertyTest");
              assertThat(ed.getProperties()).containsEntry("key", "value");
              assertThat(ed.getMeasurements()).containsEntry("key", 1.0);
            });
  }

  @Test
  @TargetUri("/throwsException")
  void testResultCodeWhenRestControllerThrows() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 1, operationId);
    List<Envelope> edList = testing.mockedIngestion.waitForItems("ExceptionData", 1);
    assertThat(edList.size()).isEqualTo(1);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rddEnvelope1 = rddList.get(0);
    Envelope edEnvelope1 = edList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(rddEnvelope1.getSampleRate()).isNull();
    assertThat(edEnvelope1.getSampleRate()).isNull();

    RequestData rd = testing.getTelemetryDataForType(0, "RequestData");
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope1.getData()).getBaseData();

    List<ExceptionDetails> details = ed.getExceptions();
    ExceptionDetails ex = details.get(0);

    assertThat(rd.getName()).isEqualTo("GET /SpringBoot/throwsException");
    assertThat(rd.getResponseCode()).isEqualTo("500");
    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd.getSuccess()).isFalse();

    assertThat(rdd1.getName()).isEqualTo("TestController.resultCodeTest");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("InProc");
    assertThat(rdd1.getTarget()).isNull();
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isFalse();

    assertThat(ex.getTypeName()).isEqualTo("javax.servlet.ServletException");
    assertThat(ex.getMessage()).isEqualTo("This is an exception");
    assertThat(ed.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
    assertThat(ed.getProperties())
        .containsKey("Logger Message"); // specific message varies by app server
    assertThat(ed.getProperties()).containsEntry("SourceType", "Logger");
    assertThat(ed.getProperties())
        .containsKey("LoggerName"); // specific logger varies by app server
    assertThat(ed.getProperties()).containsKey("ThreadName");
    assertThat(ed.getProperties()).hasSize(4);

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope1, "GET /SpringBoot/throwsException");
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, rddEnvelope1, "GET /SpringBoot/throwsException");
  }

  @Test
  @TargetUri("/asyncDependencyCall")
  void testAsyncDependencyCall() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> rddList =
        testing.mockedIngestion.waitForItemsInOperation("RemoteDependencyData", 2, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope rddEnvelope1 = rddList.get(0);
    Envelope rddEnvelope2 = rddList.get(1);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(rddEnvelope1.getSampleRate()).isNull();
    assertThat(rddEnvelope2.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    RemoteDependencyData rdd1 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope1.getData()).getBaseData();
    RemoteDependencyData rdd2 =
        (RemoteDependencyData) ((Data<?>) rddEnvelope2.getData()).getBaseData();

    assertThat(rd.getName()).isEqualTo("GET /SpringBoot/asyncDependencyCall");
    assertThat(rd.getResponseCode()).isEqualTo("200");
    assertThat(rd.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rd.getSuccess()).isTrue();

    assertThat(rdd1.getName()).isEqualTo("TestController.asyncDependencyCall");
    assertThat(rdd1.getData()).isNull();
    assertThat(rdd1.getType()).isEqualTo("InProc");
    assertThat(rdd1.getTarget()).isNull();
    assertThat(rdd1.getProperties()).isEmpty();
    assertThat(rdd1.getSuccess()).isTrue();

    assertThat(rdd2.getName()).isEqualTo("GET /");
    assertThat(rdd2.getData()).isEqualTo("https://www.bing.com");
    assertThat(rdd2.getTarget()).isEqualTo("www.bing.com");
    assertThat(rdd2.getProperties())
        .containsExactly(entry("_MS.ProcessedByMetricExtractors", "True"));
    assertThat(rdd2.getSuccess()).isTrue();

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, rddEnvelope1, "GET /SpringBoot/asyncDependencyCall");
    SmokeTestExtension.assertParentChild(
        rdd1, rddEnvelope1, rddEnvelope2, "GET /SpringBoot/asyncDependencyCall");
  }
}
