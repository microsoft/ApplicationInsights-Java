package com.microsoft.applicationinsights.testapps.perf.servlets;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet({"/fakeRest"})
public class FakeRestServlet extends APerfTestServlet {
    @Override
    protected void reallyDoGet(HttpServletRequest req, HttpServletResponse resp) {
        TelemetryClient tc = new TelemetryClient();
        tc.trackDependency("FakeRestDependency", "fakeRestCommand", new Duration(123L), true);
        tc.trackEvent("FakeRestEvent");
        tc.trackMetric("FakeRestMetric", 1.0);
        tc.trackTrace("FakeRestTrace");
    }
}