package com.microsoft.ajl.simplecalc;

import javax.servlet.ServletException;
import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;

@WebServlet(description = "Performs given calculation", urlPatterns = { "/trackDependency" })
public class SimpleTrackDependencyServlet extends HttpServlet {

    private final TelemetryClient client = new TelemetryClient();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        client.trackDependency("DependencyTest", "commandName", new Duration(0, 0, 1, 1, 1), true);
    }
}