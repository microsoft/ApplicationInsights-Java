package com.microsoft.applicationinsights.tests.manual;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.datacontracts.*;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("ALL")
class Program
{
    public static void main(String[] args) throws IOException
    {
        TelemetryClient appInsights = new TelemetryClient();
        appInsights.getContext().getProperties().put("programmatic", "works");

        Map<String, Double> metrics = new HashMap<>();
        metrics.put("Answers", (double)15);

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

        try
        {
            throwException("This is only a test!");
        }
        catch (Exception exc)
        {
            appInsights.trackException(exc);
        }

//        RemoteDependencyTelemetry rdt = new RemoteDependencyTelemetry("MongoDB");
//        rdt.setCount(1);
//        rdt.setValue(0.345);
//        rdt.setDependencyKind(DependencyKind.Other);
//        rdt.setAsync(false);
//
//        appInsights.track(rdt);

        System.out.println("Press Enter to terminate...");
        System.in.read();
    }

    private static void throwException(String msg) throws Exception
    {
        throw new Exception(msg);
    }
}
