// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.smoketest;

import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_11_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_17;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_19;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_20;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.TOMCAT_8_JAVA_8_OPENJ9;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8;
import static com.microsoft.applicationinsights.smoketest.EnvironmentValue.WILDFLY_13_JAVA_8_OPENJ9;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.applicationinsights.smoketest.schemav2.AvailabilityData;
import com.microsoft.applicationinsights.smoketest.schemav2.Data;
import com.microsoft.applicationinsights.smoketest.schemav2.DataPoint;
import com.microsoft.applicationinsights.smoketest.schemav2.Duration;
import com.microsoft.applicationinsights.smoketest.schemav2.Envelope;
import com.microsoft.applicationinsights.smoketest.schemav2.EventData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionData;
import com.microsoft.applicationinsights.smoketest.schemav2.ExceptionDetails;
import com.microsoft.applicationinsights.smoketest.schemav2.MessageData;
import com.microsoft.applicationinsights.smoketest.schemav2.MetricData;
import com.microsoft.applicationinsights.smoketest.schemav2.PageViewData;
import com.microsoft.applicationinsights.smoketest.schemav2.RequestData;
import com.microsoft.applicationinsights.smoketest.schemav2.SeverityLevel;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@UseAgent
abstract class CoreAndFilter3xTest {

  @RegisterExtension static final SmokeTestExtension testing = SmokeTestExtension.create();

  @Test
  @TargetUri("/trackDependency")
  void trackDependency() throws Exception {
    Telemetry telemetry = testing.getTelemetry(1);

    Duration expectedDuration = new Duration(0, 0, 1, 1, 1);

    assertThat(telemetry.rdd1.getName()).isEqualTo("DependencyTest");
    assertThat(telemetry.rdd1.getData()).isEqualTo("commandName");
    assertThat(telemetry.rdd1.getType()).isNull();
    assertThat(telemetry.rdd1.getTarget()).isNull();
    assertThat(telemetry.rdd1.getProperties()).isEmpty();
    assertThat(telemetry.rdd1.getSuccess()).isTrue();

    assertThat(telemetry.rdd1.getDuration()).isEqualTo(expectedDuration);

    SmokeTestExtension.assertParentChild(
        telemetry.rd,
        telemetry.rdEnvelope,
        telemetry.rddEnvelope1,
        "GET /CoreAndFilter3x/trackDependency");
  }

  @Test
  @TargetUri("/trackEvent")
  void testTrackEvent() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("EventData", 2, operationId);

    Envelope edEnvelope1 = edList.get(0);
    Envelope edEnvelope2 = edList.get(1);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(edEnvelope1.getSampleRate()).isNull();
    assertThat(edEnvelope2.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<EventData> events = testing.mockedIngestion.getTelemetryDataByTypeInRequest("EventData");
    events.sort(Comparator.comparing(EventData::getName));

    EventData ed1 = events.get(0);
    EventData ed2 = events.get(1);

    assertThat(ed1.getName()).isEqualTo("EventDataPropertyTest");
    assertThat(ed1.getProperties()).containsEntry("key", "value");
    assertThat(ed1.getMeasurements()).containsEntry("key", 1.0);

    assertThat(ed2.getName()).isEqualTo("EventDataTest");

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope1, "GET /CoreAndFilter3x/trackEvent");
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope2, "GET /CoreAndFilter3x/trackEvent");
  }

  @Test
  @TargetUri("/trackException")
  void testTrackException() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItemsInOperation("ExceptionData", 3, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope edEnvelope1 = edList.get(0);
    Envelope edEnvelope2 = edList.get(1);
    Envelope edEnvelope3 = edList.get(2);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(edEnvelope1.getSampleRate()).isNull();
    assertThat(edEnvelope2.getSampleRate()).isNull();
    assertThat(edEnvelope3.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    final String expectedName = "This is track exception.";
    final String expectedProperties = "value";
    final Double expectedMetrice = 1d;

    List<ExceptionData> exceptions =
        testing.mockedIngestion.getTelemetryDataByTypeInRequest("ExceptionData");
    assertThat(exceptions)
        .anySatisfy(
            e ->
                assertThat(e.getExceptions())
                    .extracting(ExceptionDetails::getMessage)
                    .contains(expectedName));
    assertThat(exceptions)
        .anySatisfy(
            e -> {
              assertThat(e.getExceptions())
                  .extracting(ExceptionDetails::getMessage)
                  .contains(expectedName);
              assertThat(e.getProperties()).containsEntry("key", expectedProperties);
              assertThat(e.getMeasurements()).containsEntry("key", expectedMetrice);
            });
    assertThat(exceptions)
        .anySatisfy(
            e -> {
              assertThat(e.getExceptions())
                  .extracting(ExceptionDetails::getMessage)
                  .contains(expectedName);
              assertThat(e.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
            });

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope1, "GET /CoreAndFilter3x/trackException");
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope2, "GET /CoreAndFilter3x/trackException");
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, edEnvelope3, "GET /CoreAndFilter3x/trackException");
  }

  @Test
  @TargetUri("/trackHttpRequest")
  void testHttpRequest() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 5);

    // TODO get HttpRequest data envelope and verify value
    List<RequestData> requests = testing.mockedIngestion.getTelemetryDataByType("RequestData");

    assertThat(requests)
        .anySatisfy(
            r -> {
              assertThat(r.getName()).isEqualTo("HttpRequestDataTest");
              assertThat(r.getResponseCode()).isEqualTo("200");
              assertThat(r.getDuration()).isEqualTo(new Duration(4711));
              assertThat(r.getSuccess()).isTrue();
            });
    assertThat(requests)
        .anySatisfy(
            r -> {
              assertThat(r.getName()).isEqualTo("PingTest");
              assertThat(r.getResponseCode()).isEqualTo("200");
              assertThat(r.getDuration()).isEqualTo(new Duration(1));
              assertThat(r.getSuccess()).isTrue();
              assertThat(r.getUrl()).isEqualTo("http://tempuri.org/ping");
            });
    assertThat(requests)
        .anySatisfy(
            r -> {
              assertThat(r.getName()).isEqualTo("FailedHttpRequest");
              assertThat(r.getResponseCode()).isEqualTo("404");
              assertThat(r.getDuration()).isEqualTo(new Duration(6666));
              assertThat(r.getSuccess()).isFalse();
            });
    assertThat(requests)
        .anySatisfy(
            r -> {
              assertThat(r.getName()).isEqualTo("FailedHttpRequest2");
              assertThat(r.getResponseCode()).isEqualTo("505");
              assertThat(r.getDuration()).isEqualTo(new Duration(8888));
              assertThat(r.getSuccess()).isFalse();
              assertThat(r.getUrl()).isEqualTo("https://www.bingasdasdasdasda.com/");
            });
  }

  @Test
  @TargetUri("/trackMetric")
  void trackMetric() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);
    List<Envelope> mdList = testing.mockedIngestion.waitForMetricItems("TimeToRespond", 1);

    Envelope rdEnvelope = rdList.get(0);
    Envelope mdEnvelope = mdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(mdEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    MetricData md = (MetricData) ((Data<?>) mdEnvelope.getData()).getBaseData();

    List<DataPoint> metrics = md.getMetrics();
    assertThat(metrics).hasSize(1);
    DataPoint dp = metrics.get(0);

    final double expectedValue = 111222333.0;
    assertThat(dp.getValue()).isEqualTo(expectedValue);
    assertThat(dp.getName()).isEqualTo("TimeToRespond");
    assertThat(dp.getMetricNamespace()).isNull();

    assertThat(dp.getCount()).isNull();
    assertThat(dp.getMin()).isNull();
    assertThat(dp.getMax()).isNull();
    assertThat(dp.getStdDev()).isNull();

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, mdEnvelope, "GET /CoreAndFilter3x/trackMetric");
  }

  @Test
  @TargetUri("/trackTrace")
  void testTrackTrace() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> mdList = testing.mockedIngestion.waitForMessageItemsInRequest(3, operationId);

    Envelope mdEnvelope1 = mdList.get(0);
    Envelope mdEnvelope2 = mdList.get(1);
    Envelope mdEnvelope3 = mdList.get(2);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(mdEnvelope1.getSampleRate()).isNull();
    assertThat(mdEnvelope2.getSampleRate()).isNull();
    assertThat(mdEnvelope3.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    List<MessageData> messages = testing.mockedIngestion.getMessageDataInRequest(3);

    assertThat(messages)
        .anySatisfy(m -> assertThat(m.getMessage()).isEqualTo("This is first trace message."));

    assertThat(messages)
        .anySatisfy(
            m -> {
              assertThat(m.getMessage()).isEqualTo("This is second trace message.");
              assertThat(m.getSeverityLevel()).isEqualTo(SeverityLevel.ERROR);
            });

    assertThat(messages)
        .anySatisfy(
            m -> {
              assertThat(m.getMessage()).isEqualTo("This is third trace message.");
              assertThat(m.getSeverityLevel()).isEqualTo(SeverityLevel.INFORMATION);
              assertThat(m.getProperties()).containsEntry("key", "value");
            });

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, mdEnvelope1, "GET /CoreAndFilter3x/trackTrace");
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, mdEnvelope2, "GET /CoreAndFilter3x/trackTrace");
    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, mdEnvelope3, "GET /CoreAndFilter3x/trackTrace");
  }

  @Test
  @TargetUri("/trackPageView")
  void testTrackPageView() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    List<Envelope> pvdList = testing.mockedIngestion.waitForItems("PageViewData", 3);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    Envelope pvdEnvelope1 = null;
    Envelope pvdEnvelope2 = null;
    Envelope pvdEnvelope3 = null;

    for (Envelope pvdEnvelope : pvdList) {
      PageViewData pv = (PageViewData) ((Data<?>) pvdEnvelope.getData()).getBaseData();
      if (pv.getName().equals("test-page")) {
        pvdEnvelope1 = pvdEnvelope;
      } else if (pv.getName().equals("test-page-2")) {
        pvdEnvelope2 = pvdEnvelope;
      } else if (pv.getName().equals("test-page-3")) {
        pvdEnvelope3 = pvdEnvelope;
      } else {
        throw new AssertionError("Unexpected page view: " + pv.getName());
      }
    }

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(pvdEnvelope1.getSampleRate()).isNull();
    assertThat(pvdEnvelope2.getSampleRate()).isNull();
    assertThat(pvdEnvelope3.getSampleRate()).isNull();

    PageViewData pv1 = (PageViewData) ((Data<?>) pvdEnvelope1.getData()).getBaseData();
    PageViewData pv2 = (PageViewData) ((Data<?>) pvdEnvelope2.getData()).getBaseData();
    PageViewData pv3 = (PageViewData) ((Data<?>) pvdEnvelope3.getData()).getBaseData();

    assertThat(pv1).isNotNull();
    assertThat(pv1.getDuration()).isEqualTo(new Duration(0));
    // checking that instrumentation key, cloud role name, cloud role instance, and sdk version are
    // from the agent
    assertThat(pvdEnvelope1.getIKey()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(pvdEnvelope1.getTags()).containsEntry("ai.cloud.role", "testrolename");
    assertThat(pvdEnvelope1.getTags()).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    assertThat(pvdEnvelope1.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("java:3."));

    assertThat(pv2).isNotNull();
    assertThat(pv2.getDuration()).isEqualTo(new Duration(123456));
    assertThat(pvdEnvelope2.getTime()).isEqualTo("2010-10-10T00:00:00Z");
    assertThat(pv2.getProperties()).containsEntry("key", "value");
    assertThat(pv2.getProperties()).containsEntry("a-prop", "a-value");
    assertThat(pv2.getProperties()).containsEntry("another-prop", "another-value");
    // operation name is verified below in assertParentChild()
    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.user.id", "user-id-goes-here");
    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.user.accountId", "account-id-goes-here");
    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.user.userAgent", "user-agent-goes-here");
    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.device.os", "os-goes-here");
    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.session.id", "session-id-goes-here");
    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.location.ip", "1.2.3.4");
    // checking that instrumentation key, cloud role name and cloud role instance are overridden
    assertThat(pvdEnvelope2.getIKey()).isEqualTo("12341234-1234-1234-1234-123412341234");
    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.cloud.role", "role-goes-here");
    assertThat(pvdEnvelope2.getTags().get("ai.cloud.roleInstance"))
        .isEqualTo("role-instance-goes-here");
    // checking that sdk version is from the agent
    assertThat(pvdEnvelope2.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("java:3."));

    assertThat(pv3).isNotNull();
    assertThat(pv3.getDuration()).isEqualTo(new Duration(123456));
    assertThat(pvdEnvelope3.getTime()).isEqualTo("2010-10-10T00:00:00Z");
    assertThat(pv3.getProperties()).containsEntry("key", "value");
    assertThat(pv3.getProperties()).containsEntry("a-prop", "a-value");
    assertThat(pv3.getProperties()).containsEntry("another-prop", "another-value");
    // operation name is verified below in assertParentChild()
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.user.id", "user-id-goes-here");
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.user.accountId", "account-id-goes-here");
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.user.userAgent", "user-agent-goes-here");
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.device.os", "os-goes-here");
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.session.id", "session-id-goes-here");
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.location.ip", "1.2.3.4");
    // checking that instrumentation key, cloud role name and cloud role instance are from the agent
    assertThat(pvdEnvelope3.getIKey()).isEqualTo("00000000-0000-0000-0000-0FEEDDADBEEF");
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.cloud.role", "testrolename");
    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.cloud.roleInstance", "testroleinstance");
    // checking that sdk version is from the agent
    assertThat(pvdEnvelope3.getTags())
        .hasEntrySatisfying("ai.internal.sdkVersion", v -> assertThat(v).startsWith("java:3."));

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, pvdEnvelope1, "GET /CoreAndFilter3x/trackPageView");

    assertThat(pvdEnvelope2.getTags()).containsEntry("ai.operation.id", "operation-id-goes-here");
    assertThat(pvdEnvelope2.getTags())
        .containsEntry("ai.operation.parentId", "operation-parent-id-goes-here");
    assertThat(pvdEnvelope2.getTags().get("ai.operation.name"))
        .isEqualTo("operation-name-goes-here");

    assertThat(pvdEnvelope3.getTags()).containsEntry("ai.operation.id", "operation-id-goes-here");
    assertThat(pvdEnvelope3.getTags())
        .containsEntry("ai.operation.parentId", "operation-parent-id-goes-here");
    assertThat(pvdEnvelope3.getTags().get("ai.operation.name"))
        .isEqualTo("operation-name-goes-here");
  }

  @Test
  @TargetUri("/doPageView.jsp")
  void testTrackPageViewJsp() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> pvdList =
        testing.mockedIngestion.waitForItemsInOperation("PageViewData", 1, operationId);
    assertThat(testing.mockedIngestion.getCountForType("EventData")).isZero();

    Envelope pvdEnvelope = pvdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(pvdEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    PageViewData pv = (PageViewData) ((Data<?>) pvdEnvelope.getData()).getBaseData();
    assertThat(pv.getName()).isEqualTo("doPageView");
    assertThat(pv.getDuration()).isEqualTo(new Duration(0));

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, pvdEnvelope, "GET /CoreAndFilter3x/doPageView.jsp");
  }

  @Test
  @TargetUri("/trackAvailability")
  void trackAvailability() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> adList =
        testing.mockedIngestion.waitForItemsInOperation("AvailabilityData", 1, operationId);

    Envelope adEnvelope = adList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(adEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    AvailabilityData pv = (AvailabilityData) ((Data<?>) adEnvelope.getData()).getBaseData();
    assertThat(pv.getId()).isEqualTo("an-id");
    assertThat(pv.getName()).isEqualTo("a-name");
    assertThat(pv.getDuration()).isEqualTo(new Duration(1234));
    assertThat(pv.getSuccess()).isTrue();
    assertThat(pv.getRunLocation()).isEqualTo("a-run-location");
    assertThat(pv.getMessage()).isEqualTo("a-message");

    SmokeTestExtension.assertParentChild(
        rd, rdEnvelope, adEnvelope, "GET /CoreAndFilter3x/trackAvailability");
  }

  @Test
  @TargetUri("/autoFailedRequestWithResultCode")
  void testAutoFailedRequestWithResultCode() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    assertThat(rd.getSuccess()).isFalse();
    assertThat(rd.getResponseCode()).isEqualTo("404");

    assertThat(rdEnvelope.getTags()).containsEntry("ai.operation.name", "GET /CoreAndFilter3x/*");
  }

  @Test
  @TargetUri("/requestSlow?sleeptime=20")
  void testRequestSlowWithResponseTime() throws Exception {
    validateSlowTest(20, "GET /CoreAndFilter3x/requestSlow");
  }

  @Test
  @TargetUri("/slowLoop?responseTime=5")
  void testSlowRequestUsingCpuBoundLoop() throws Exception {
    validateSlowTest(5, "GET /CoreAndFilter3x/slowLoop");
  }

  @Test
  @TargetUri("/autoExceptionWithFailedRequest")
  void testAutoExceptionWithFailedRequest() throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);
    String operationId = rdEnvelope.getTags().get("ai.operation.id");
    List<Envelope> edList =
        testing.mockedIngestion.waitForItems(
            new Predicate<Envelope>() {
              @Override
              public boolean test(Envelope input) {
                if (!"ExceptionData".equals(input.getData().getBaseType())) {
                  return false;
                }
                if (!operationId.equals(input.getTags().get("ai.operation.id"))) {
                  return false;
                }
                // lastly, filter out ExceptionData captured from tomcat logger
                ExceptionData data = (ExceptionData) ((Data<?>) input.getData()).getBaseData();
                return !data.getProperties().containsKey("LoggerName");
              }
            },
            1,
            10,
            SECONDS);

    Envelope edEnvelope = edList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();
    assertThat(edEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();
    ExceptionData ed = (ExceptionData) ((Data<?>) edEnvelope.getData()).getBaseData();

    assertThat(rd.getSuccess()).isFalse();

    ExceptionDetails details = getExceptionDetails(ed);
    assertThat(details.getMessage()).isEqualTo("This is a auto thrown exception !");
  }

  @Test
  @TargetUri("/index.jsp")
  void testRequestJsp() throws Exception {
    testing.mockedIngestion.waitForItems("RequestData", 1);
  }

  private static ExceptionDetails getExceptionDetails(ExceptionData exceptionData) {
    List<ExceptionDetails> details = exceptionData.getExceptions();
    return details.get(0);
  }

  private void validateSlowTest(int expectedDurationSeconds, String operationName)
      throws Exception {
    List<Envelope> rdList = testing.mockedIngestion.waitForItems("RequestData", 1);

    Envelope rdEnvelope = rdList.get(0);

    assertThat(rdEnvelope.getSampleRate()).isNull();

    RequestData rd = (RequestData) ((Data<?>) rdEnvelope.getData()).getBaseData();

    long actual = rd.getDuration().getTotalMilliseconds();
    long expected = SECONDS.toMillis(expectedDurationSeconds);
    long tolerance = 2000; // 2 seconds

    long min = expected - tolerance;
    long max = expected + tolerance;

    System.out.printf("Slow response time: expected=%d, actual=%d%n", expected, actual);
    assertThat(actual).isGreaterThanOrEqualTo(min);
    assertThat(actual).isLessThan(max);

    assertThat(rdEnvelope.getTags()).containsEntry("ai.operation.name", operationName);
  }

  @Environment(TOMCAT_8_JAVA_8)
  static class Tomcat8Java8Test extends CoreAndFilter3xTest {}

  @Environment(TOMCAT_8_JAVA_8_OPENJ9)
  static class Tomcat8Java8OpenJ9Test extends CoreAndFilter3xTest {}

  @Environment(TOMCAT_8_JAVA_11)
  static class Tomcat8Java11Test extends CoreAndFilter3xTest {}

  @Environment(TOMCAT_8_JAVA_11_OPENJ9)
  static class Tomcat8Java11OpenJ9Test extends CoreAndFilter3xTest {}

  @Environment(TOMCAT_8_JAVA_17)
  static class Tomcat8Java17Test extends CoreAndFilter3xTest {}

  @Environment(TOMCAT_8_JAVA_19)
  static class Tomcat8Java19Test extends CoreAndFilter3xTest {}

  @Environment(TOMCAT_8_JAVA_20)
  static class Tomcat8Java20Test extends CoreAndFilter3xTest {}

  @Environment(WILDFLY_13_JAVA_8)
  static class Wildfly13Java8Test extends CoreAndFilter3xTest {}

  @Environment(WILDFLY_13_JAVA_8_OPENJ9)
  static class Wildfly13Java8OpenJ9Test extends CoreAndFilter3xTest {}
}
