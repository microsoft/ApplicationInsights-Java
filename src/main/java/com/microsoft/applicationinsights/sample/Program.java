package com.microsoft.applicationinsights.sample;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.DefaultTelemetryClient;
import com.microsoft.applicationinsights.telemetry.HttpRequestTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

@SuppressWarnings("ALL")
class Program {
    public static void main(String[] args) throws IOException {
        validateCore();
        traceLog4J12();
        traceLog4J2();
        traceLogback();

        System.out.println("Press Enter to terminate...");
        System.in.read();
    }

    // region Logging

    private static void traceLog4J12() {
        org.apache.log4j.Logger logger = org.apache.log4j.LogManager.getRootLogger();
        logger.trace("New Log4j 1.2 event!");
    }

    private static void traceLog4J2() {
        org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getRootLogger();
        logger.trace("New Log4j 2 event!");
    }

    private static void traceLogback() {

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) org.slf4j.LoggerFactory.getLogger("root");
        logger.trace("New Logback event!");
    }

    // endregion Logging

    // region Core

    private static void validateCore() throws IOException {
        TelemetryClient appInsights = new DefaultTelemetryClient();
        appInsights.getContext().getProperties().put("programmatic", "works");

        appInsights.trackPageView("default page");

        Map<String, Double> metrics = new HashMap<String, Double>();
        metrics.put("Answers", (double) 15);

        appInsights.trackEvent("A test event", null, metrics);

        appInsights.trackTrace("Things seem to be going well");

        MetricTelemetry mt = new MetricTelemetry("Test time", 17.0);
        mt.setMax(20.0);
        mt.setMin(10.0);
        mt.setCount(100);
        mt.setStandardDeviation(2.43);
        appInsights.trackMetric(mt);

        HttpRequestTelemetry rt = new HttpRequestTelemetry("ping", new Date(), 4711, "200", true);
        rt.setHttpMethod("GET");
        rt.setUrl("http://tempuri.org/ping");
        appInsights.track(rt);

        try {
            throwException("This is only a test!");
        } catch (Exception exc) {
            appInsights.trackException(exc);
        }
    }

    private static void throwException(String msg) throws Exception {
        throw new Exception(msg);
    }

    // endregion Core
}
