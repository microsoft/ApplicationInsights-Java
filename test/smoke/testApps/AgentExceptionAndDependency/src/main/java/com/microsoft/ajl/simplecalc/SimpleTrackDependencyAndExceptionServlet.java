package com.microsoft.ajl.simplecalc;

import javax.servlet.ServletException;
import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;

/**
 * Servlet implementation class SimpleTrackDependencyAndExceptionServlet
 */
@WebServlet(description = "Performs given calculation", urlPatterns = { "/trackData" })
public class SimpleTrackDependencyAndExceptionServlet extends HttpServlet {    
	private static final long serialVersionUID = -7496476539225639976L;
	private TelemetryClient client = new TelemetryClient();

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        ServletFuncs.getRrenderHtml(request, response);
        Exception exception = new Exception("This is track exception.");
        client.trackException(exception);
        client.trackDependency("AgentDependencyTest", "commandName", new Duration(0, 0, 1, 1, 1), true);
    }
}