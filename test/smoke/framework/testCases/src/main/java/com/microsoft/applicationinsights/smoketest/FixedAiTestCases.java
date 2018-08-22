package com.microsoft.applicationinsights.smoketest;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;
import com.microsoft.applicationinsights.telemetry.PageViewTelemetry;
import com.microsoft.applicationinsights.telemetry.RemoteDependencyTelemetry;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class FixedAiTestCases {

    private final CustomAiTestCases customCases;


    public FixedAiTestCases(TelemetryClient tclient) {
        this.customCases = new CustomAiTestCases(tclient);
    }

    private Map<String, String> getPropertyMapForMethod(String method) {
        Preconditions.checkNotNull(method);
        return new HashMap<String, String>() {{
            put(String.format("Test%sProp1", method), String.format("Test%sP1_Value", method));
            put(String.format("Test%sProp2", method), String.format("Test%sP2_Value", method));
        }};
    }

    private Map<String, Double> getMetricMapForMethod(String method) {
        return new HashMap<String, Double>() {{
            put(String.format("Test%sMetric1", method), 555.555);
            put(String.format("Test%sMetric2", method), 666.666);
        }};
    }

    public Runnable getTrackEvent() {
        return customCases.getTrackEvent("AiTestEvent", getPropertyMapForMethod("Event"), getMetricMapForMethod("Event"));
    }

    public Runnable getTrackTrace() {
        return customCases.getTrackTrace("AiTestTrace", SeverityLevel.Warning, getPropertyMapForMethod("Trace"));
    }

    public Runnable getTrackMetric_Aggregate() {
        final double value = (123.4 + 567.8);
        final int count = 2;
        final double min = 123.4;
        final double max = 567.8;
        final double avg = value/count;
        final double stdDev = Math.sqrt(((123.4-avg)*(123.4-avg) + (567.8-avg)*(567.8-avg))/count);
        MetricTelemetry mt = new MetricTelemetry();
        mt.setName("AiTestMetric_Aggregate");
        mt.setValue(value);
        mt.setCount(count);
        mt.setMin(min);
        mt.setStandardDeviation(stdDev);
        for (Entry<String, String> entry : getPropertyMapForMethod("Metric_Agg").entrySet()) {
            mt.getProperties().put(entry.getKey(), entry.getValue());
        }

        return customCases.getTrackMetric(mt);
    }

    public Runnable getTrackMetric_Measurement() {
        final double value = 789.0123;
        final int count = 1;
        MetricTelemetry mt = new MetricTelemetry();
        mt.setName("AiTestMetric_Measurement");
        mt.setValue(value);
        mt.setCount(count);
        for (Entry<String, String> entry : getPropertyMapForMethod("Metric_Mea").entrySet()) {
            mt.getProperties().put(entry.getKey(), entry.getValue());
        }

        return customCases.getTrackMetric(mt);
    }

    public Runnable getTrackException() {
        return customCases.getTrackException(new Exception("AiTestException", new Exception("TestExceptionCause")), getPropertyMapForMethod("Exception"), getMetricMapForMethod("Exception"));
    }

    /**
     * Generates random result code >= 200 and code < 300.
     * @return
     */
    public Runnable getTrackHttpRequest_Success() {
        Random r = new Random(System.currentTimeMillis());
        int code = 200 + r.nextInt(100);
        return customCases.getTrackHttpRequest("AiTestHttpRequest1", Date.from(Instant.now()), 123L, String.valueOf(code), true);
    }

    /**
     * Generates random result code >= 400
     * @return
     */
    public Runnable getTrackHttpRequest_Failed() {
        Random r = new Random(System.currentTimeMillis());
        int code = 400 + r.nextInt(200);
        return customCases.getTrackHttpRequest("AiTestHttpRequest2", Date.from(Instant.now()), 456L, String.valueOf(code), false);
    }

    /**
     * Uses 100 as result code. Success=true
     * @return
     */
    public Runnable getTrackRequest() {
        // name, timestamp, duration, resultCode, success
        RequestTelemetry rt = new RequestTelemetry("AiTestRequest", Date.from(Instant.now()), 147L, "100", true);
        // add props
        for (Entry<String, String> entry : getPropertyMapForMethod("Request").entrySet()) {
            rt.getProperties().put(entry.getKey(), entry.getValue());
        }
        // set URL
        try {
            rt.setUrl(new URL("http", "some-host.somewhere", 9997, "some/file/path.ext"));
        } catch (MalformedURLException e) {
            throw new RuntimeException("The url format is messed up. fix it.", e);
        }
        // set Source
        rt.setSource("some-source-for-test-request");

        return customCases.getTrackRequest(rt);
    }

    /**
     * success=true
     * @return
     */
    public Runnable getTrackDependency() {
        return customCases.getTrackDependency("AiTestDependency", "TestCommand1", new Duration(789L), true);
    }

    /**
     * Success=false
     * @return
     */
    public Runnable getTrackDependency_Full() {
        RemoteDependencyTelemetry rdt = new RemoteDependencyTelemetry("AiTestDependency");
        rdt.setCommandName("TestCommand2");
        rdt.setDuration(new Duration(999L));
        rdt.setResultCode("503");
        rdt.setSuccess(false);
        rdt.setTarget("some-target");
        rdt.setTarget("fake-type");
        for (Entry<String, String> entry : getPropertyMapForMethod("Dependency").entrySet()) {
            rdt.getProperties().put(entry.getKey(), entry.getValue());
        }
        for (Entry<String, Double> entry : getMetricMapForMethod("Depdenency").entrySet()) {
            rdt.getMetrics().put(entry.getKey(), entry.getValue());
        }
        return customCases.getTrackDependency(rdt);
    }

    public Runnable getTrackPageView() {
        return customCases.getTrackPageView("AiTestPageView1");
    }

    public Runnable getTrackPageView_Full() {
        PageViewTelemetry pvt = new PageViewTelemetry("AiTestPageView2");
        pvt.setDuration(1011L);
        pvt.setUrl(URI.create("some-host.somewhere/fake/path/elements/AiTestPageView2.html"));
        
        for (Entry<String, String> entry : getPropertyMapForMethod("PageView").entrySet()) {
            pvt.getProperties().put(entry.getKey(), entry.getValue());
        }
        for (Entry<String, Double> entry : getMetricMapForMethod("PageView").entrySet()) {
            pvt.getMetrics().put(entry.getKey(), entry.getValue());
        }

        return customCases.getTrackPageView(pvt);
    }

}
