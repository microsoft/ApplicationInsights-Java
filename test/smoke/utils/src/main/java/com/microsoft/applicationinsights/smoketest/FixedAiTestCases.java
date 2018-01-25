package com.microsoft.applicationinsights.smoketest;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class FixedAiTestCases {

    private final TelemetryClient tclient;


    public FixedAiTestCases(TelemetryClient tclient) {
        this.tclient = tclient;
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
        return new Runnable() {
            @Override
            public void run() {
                tclient.trackEvent("AiTestEvent", getPropertyMapForMethod("Event"), getMetricMapForMethod("Event"));
            }
        };
    }

    public Runnable getTrackTrace() {
        return new Runnable() {
            @Override
            public void run() {
                tclient.trackTrace("TestTrace", SeverityLevel.Warning, getPropertyMapForMethod("Trace"));
            }
        };
    }

    public Runnable getTrackMetric() {
        return new Runnable(){
            @Override
            public void run() {
                tclient.trackMetric("TestMetric", (123.4 + 567.8)/2.0, 2, 123.4, 567.8, getPropertyMapForMethod("Metric"));
            }
        };
    }

    public Runnable getTrackException() {
        return new Runnable(){
            @Override
            public void run() {
                tclient.trackException(new Exception("TestException", new Exception("TestExceptionCause")), getPropertyMapForMethod("Exception"), getMetricMapForMethod("Exception"));
            }
        };
    }

    public Runnable getTrackHttpRequest() {
        return new Runnable(){
            @Override
            public void run() {
                tclient.trackHttpRequest("TestHttpRequest", new Date(), 123L, "200", true);
            }
        };
    }

    public Runnable getTrackDependency() {
        return new Runnable(){
            @Override
            public void run() {
                tclient.trackDependency("TestDependency", "TestCommand", new Duration(0, 0, 0, 12, 345), true);
            }
        };
    }

    public Runnable getTrackPageView() {
        return new Runnable(){
            @Override
            public void run() {
                tclient.trackPageView("TestPageView");
            }
        };
    }

}
