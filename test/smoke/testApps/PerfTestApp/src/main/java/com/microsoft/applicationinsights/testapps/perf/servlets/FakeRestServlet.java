package com.microsoft.applicationinsights.testapps.perf.servlets;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.testapps.perf.TestCaseRunnable;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet({"/fakeRest"})
public class FakeRestServlet extends APerfTestServlet {
    @Override
    protected void reallyDoGet(HttpServletRequest req, HttpServletResponse resp) {
        new TestCaseRunnable(new Runnable() {
            @Override
            public void run() {
                TelemetryClient tc = new TelemetryClient();
                tc.trackDependency("FakeRestDependency", "fakeRestCommand", new Duration(123L), true);
                tc.trackEvent("FakeRestEvent");
                tc.trackMetric("FakeRestMetric", 1.0);
                tc.trackTrace("FakeRestTrace");
            }
        }, "fakeRest operation").run();
    }
}