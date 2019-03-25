package com.microsoft.applicationinsights.controller;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class TestController {

    private CloseableHttpClient closeableHttpClient = HttpClients.createDefault();

    private TelemetryClient telemetryClient;

    public TestController(TelemetryClient client) {
        this.telemetryClient = client;
    }

    @GetMapping("/trackDependencyAutomatically")
    public int trackDependencyAutomatically() throws IOException {
        HttpGet httpGet = new HttpGet("https://www.google.com");
        try (CloseableHttpResponse response = closeableHttpClient.execute(httpGet)) {
            response.getStatusLine().getStatusCode();
        }
        // Failure
        return 500;
    }

    @GetMapping("/trackManualEvent")
    public void trackManualEvent() {
        EventTelemetry telemetry = new EventTelemetry("Some event occurred");
        telemetryClient.trackEvent(telemetry);
    }
}
