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

/**
 * Servlet implementation class SimpleCalculatorServlet
 */
@WebServlet(description = "Performs given calculation", urlPatterns = { "/trackEvent" })
public class SimpleTrackEventServlet extends HttpServlet {
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
		Map<String, Double> metrics = new HashMap<String, Double>() {
			{
				put("key", 1d);
			}
		};

		//Event
		client.trackEvent("EventDataTest");
		client.trackEvent("EventDataPropertyTest", properties, metrics);
	}
}