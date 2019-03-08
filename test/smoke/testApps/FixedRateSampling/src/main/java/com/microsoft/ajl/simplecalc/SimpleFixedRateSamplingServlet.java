package com.microsoft.ajl.simplecalc;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;

/**
 * Servlet implementation class SimpleFixedRateSamplingServlet
 */
@WebServlet(description = "Performs given calculation", urlPatterns = { "/fixedRateSampling" })

public class SimpleFixedRateSamplingServlet extends HttpServlet {
    private static final long serialVersionUID = -5889330779672565409L;
    private TelemetryClient client = new TelemetryClient();

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.getRenderedHtml(request, response);
        client.trackTrace("Trace Test.");
        int i = 0;
        while (i < 100) {
            String str = String.format("Event Test %s", i);
            client.trackEvent(str);
            i++;
        }        
    }
}