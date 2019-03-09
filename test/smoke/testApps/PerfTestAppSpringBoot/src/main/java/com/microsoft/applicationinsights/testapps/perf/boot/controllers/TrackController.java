package com.microsoft.applicationinsights.testapps.perf.boot.controllers;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.smoketest.FixedAiTestCases;
import com.microsoft.applicationinsights.testapps.perf.TestCaseRunnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static com.microsoft.applicationinsights.testapps.perf.boot.SpringBootPerfTestHelper.runTest;

@RestController
@RequestMapping("/track")
public class TrackController {


    private FixedAiTestCases testCases;
    @Autowired
    private HttpServletRequest request;

    public TrackController(TelemetryClient telemetryClient) {
        testCases = new FixedAiTestCases(telemetryClient);
    }

    private void printUri() {
        System.out.println("GET "+request.getRequestURI());
    }

    @GetMapping("/metric/{level}/{type}")
    public String trackMetric(@PathVariable String level, @PathVariable String type) {
        printUri();
        final TestCaseRunnable tcr;
        String name = String.format("metric %s %s", level, type);
        if ("full".equals(level) && "measurement".equals(type)) {
            tcr = new TestCaseRunnable(testCases.getTrackMetric_FullMeasurement(), name);
        } else if ("full".equals(level) && "aggregate".equals(type)) {
            tcr = new TestCaseRunnable(testCases.getTrackMetric_FullAggregate(), name);
        } else if ("helper".equals(level) && "measurement".equals(type)) {
            tcr = new TestCaseRunnable(testCases.getTrackMetric_HelperMeasurement(), name);
        } else if ("helper".equals(level) && "aggregate".equals(type)) {
            tcr = new TestCaseRunnable(testCases.getTrackMetric_HelperAggregate(), name);
        } else {
            throw new UnsupportedOperationException("unknown level/type pair: "+name);
        }

        return runTest(tcr);
    }

    @GetMapping("/event")
    public String trackEvent() {
        printUri();
        return runTest(new TestCaseRunnable(testCases.getTrackEvent(), "event"));
    }

    @GetMapping("/request/full")
    public String trackRequestFull() {
        printUri();
        return runTest(new TestCaseRunnable(testCases.getTrackRequest_Full(), "request full"));
    }

    @GetMapping("/httpRequest")
    public String trackHttpRequest() {
        printUri();
        return runTest(new TestCaseRunnable(testCases.getTrackHttpRequest_Success(), "http request"));
    }

    @GetMapping({"/dependency/{level}", "/dependency"})
    public String trackDependency(@PathVariable(value = "level", required = false) String level) {
        printUri();
        final TestCaseRunnable tcr;
        if ("full".equals(level)) {
            tcr = new TestCaseRunnable(testCases.getTrackDependency_Full(), "dependency full");
        } else if (level == null) {
            tcr = new TestCaseRunnable(testCases.getTrackDependency(), "dependency simple");
        } else {
            throw new UnsupportedOperationException("Unknown dependency level: "+level);
        }
        return runTest(tcr);
    }

    @GetMapping("/exception")
    public String trackException() {
        printUri();
        return runTest(new TestCaseRunnable(testCases.getTrackException(), "exception"));
    }

    @GetMapping("/trace")
    public String trackTrace() {
        printUri();
        return runTest(new TestCaseRunnable(testCases.getTrackTrace(), "trace"));
    }

    @GetMapping({"/pageView/{level}", "/pageView"})
    public String trackPageView(@PathVariable(value = "level", required = false) String level) {
        printUri();
        final TestCaseRunnable tcr;
        if ("full".equals(level)) {
            tcr = new TestCaseRunnable(testCases.getTrackPageView_Full(), "pageView full");
        } else if (level == null) {
            tcr = new TestCaseRunnable(testCases.getTrackPageView(), "pageView simple");
        } else {
            throw new UnsupportedOperationException("Unknown pageView level: "+level);
        }
        return runTest(tcr);
    }
}
