package com.microsoft.ajl.simplecalc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

/**
 * Servlet implementation class SimpleCalculatorServlet2
 */
@WebServlet(description = "Performs given calculation", urlPatterns = { "/trackTrace" })
public class SimpleTrackTraceServlet extends HttpServlet {
    private static final long serialVersionUID = -633683109556605395L;
    private TelemetryClient client = new TelemetryClient();

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.geRrenderHtml(request, response);

        Map<String, String> properties = new HashMap<String, String>() {
            {
                put("key", "value");
            }
        };
        //Trace
        client.trackTrace("This is first trace message.");
        client.trackTrace("This is second trace message.", SeverityLevel.Error, null);
        client.trackTrace("This is third trace message.", SeverityLevel.Information, properties);
    }
}