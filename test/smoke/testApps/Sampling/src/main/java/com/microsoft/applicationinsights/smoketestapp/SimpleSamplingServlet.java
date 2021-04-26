package com.microsoft.applicationinsights.smoketestapp;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

@WebServlet(description = "sends 100 event telemetry items with different op ids", urlPatterns = { "/sampling" })
public class SimpleSamplingServlet extends HttpServlet {

    private final TelemetryClient client = new TelemetryClient();

    private final AtomicInteger count = new AtomicInteger();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ServletFuncs.getRenderedHtml(request, response);

        client.trackEvent(new EventTelemetry("Event Test " + count.getAndIncrement()));
    }
}
