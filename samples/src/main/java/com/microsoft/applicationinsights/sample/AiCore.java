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

package com.microsoft.applicationinsights.sample;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.TelemetryConfiguration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.MetricTelemetry;

@SuppressWarnings("ALL")
public class AiCore {
    public static void main(String[] args) throws IOException {
        System.out.println("This program sends various application insights telemetry events.");

        TelemetryClient appInsights = new TelemetryClient();
        if (args.length > 0) {
            appInsights.getContext().setInstrumentationKey(args[0]);
        }
        String iKey = appInsights.getContext().getInstrumentationKey();
        if (iKey == null)
        {
            System.out.println("Error: no iKey set in ApplicationInsights.xml or as a parameter for this program.");
            return;
        }
        System.out.println("Application iKey set to " + appInsights.getContext().getInstrumentationKey());
        TelemetryConfiguration.getActive().getChannel().setDeveloperMode(true);

        System.out.println();
        appInsights.getContext().getProperties().put("programmatic", "works");
        System.out.println("Set context property      -- programmatic=works");

        appInsights.trackPageView("default page");
        System.out.println("[1] PageView              -- page=\"default page\"");

        // Tracking metrics event
        Map<String, Double> metrics = new HashMap<String, Double>();
        metrics.put("Answers", (double) 15);
        appInsights.trackEvent("A test event", null, metrics);
        System.out.println("[2] Custom Event (metric) -- name=\"A test event\", metric:Answers=15");

        // Trace telemetry
        appInsights.trackTrace("Things seem to be going well");
        System.out.println("[3] Trace                 -- text=\"Things seem to be going well\"");

        // Metric Telemetry
        MetricTelemetry mt = new MetricTelemetry("Test time", 17.0);
        mt.setMax(20.0);
        mt.setMin(10.0);
        mt.setCount(100);
        mt.setStandardDeviation(2.43);
        appInsights.trackMetric(mt);
        System.out.println("[4] Metric                -- metric:\"Test time\", value=17.0, Max=20.0, Min=10.0, Count=100 and STDV=2.43");

        // Http Request Telemetry
        RequestTelemetry rt = new RequestTelemetry("ping", new Date(), 4711, "200", true);
        rt.setHttpMethod("GET");
        rt.setUrl("http://tempuri.org/ping");
        appInsights.track(rt);
        System.out.println("[5] HttpRequest           -- url=\"http://tempuri.org/ping\", HttpMethod=\"GET\", request=\"ping\", duration=4711, response=200 and success=true");

        // Tracking Exception
        try {
            throw new Exception("This is only a test!");
        } catch (Exception exc) {
            appInsights.trackException(exc);
            System.out.println("[6] Exception             -- message=\"This is only a test!\"");
        }

        System.out.println();
        System.out.println("Press any key to exit");
        System.in.read();
    }

    // endregion Core
}
