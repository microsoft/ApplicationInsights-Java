package com.microsoft.ajl.simplecalc;

import com.microsoft.applicationinsights.TelemetryClient;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet(description = "Performs given calculation", urlPatterns = { "/trackMetric" })
public class SimpleTrackMetricServlet extends HttpServlet {

    private final TelemetryClient client = new TelemetryClient();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        client.trackMetric("TimeToRespond", 111222333);
    }
}