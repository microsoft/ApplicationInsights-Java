package com.microsoft.applicationinsights.testapps.coreonly;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.smoketest.AiTestCases;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.NotImplementedException;

@WebServlet(urlPatterns = "/track/*")
public class TrackServlet extends HttpServlet {
	private volatile TelemetryClient tc;
	private volatile AiTestCases cases;
	private static final Object lock = new Object();
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		initVars();
		String path = req.getPathInfo();
		Map<String, String[]> params = req.getParameterMap();
		final TestCaseRunnable tcr;
		try {
			validateRequiredParameters(path, req);
			boolean hasOptionalParameters = validateOptionalParameters(path, req);
			switch (path) {
				case "metric":
					if (hasOptionalParameters) {
						tcr = new TestCaseRunnable(cases.getTrackMetric(
							req.getParameter("name"), 
							Double.valueOf(req.getParameter("value")), 
							Integer.valueOf(req.getParameter("sampleCount")), 
							Double.valueOf(req.getParameter("min")), 
							Double.valueOf(req.getParameter("max")),
							null));
					}
					else {
						tcr = new TestCaseRunnable(cases.getTrackMetric(
							req.getParameter("name"), 
							Double.valueOf(req.getParameter("value"))));							
					}
					break;
				case "event":
					throw new NotImplementedException("track/event");
				case "trace":
					tcr = new TestCaseRunnable(cases.getTrackTrace(
						req.getParameter("message"), 
						SeverityLevel.valueOf(req.getParameter("severityLevel")), 
						null));
					break;
				case "exception":
					throw new NotImplementedException("track/exception");
				case "request":
					throw new NotImplementedException("track/request");
				case "httprequest":
					tcr = new TestCaseRunnable(cases.getTrackHttpRequest(
						req.getParameter("name"), 
						new SimpleDateFormat().parse(req.getParameter("timestamp")),
						Long.valueOf(req.getParameter("duration")), 
						req.getParameter("responseCode"), 
						Boolean.parseBoolean(req.getParameter("success"))));
					break;
				case "dependency":
					tcr = new TestCaseRunnable(cases.getTrackDependency(
						req.getParameter("dependencyName"), 
						req.getParameter("commandName"), 
						new Duration(Long.parseLong(req.getParameter("duration"))), 
						Boolean.parseBoolean(req.getParameter("success"))));
				default:
					resp.sendError(404, "unknown path: "+path);
					return;
			}
		} catch (IOException ioe) {
			throw ioe;
		} catch (IllegalArgumentException iae) {
			iae.printStackTrace();
			resp.sendError(400, iae.getLocalizedMessage());
			return;
		} catch (ParseException e) {
			e.printStackTrace();
			resp.sendError(400, e.getLocalizedMessage());
			return;
		}
		tcr.run();
	}

	private void validateRequiredParameters(String path, HttpServletRequest req) {
		String[] rp = null;
		switch (path) {
			case "metric":
				rp = new String[] { "name", "value" };
				break;
			case "event":
				break;
			case "trace":
				rp = new String[] { "message", "severityLevel" };
				break;
			case "exception":
				break;
			case "request":
				break;
			case "httprequest":
				rp = new String[] { "name", "timestamp", "duration", "responseCode", "success" };
				break;
			case "dependency":
				rp = new String[] { "dependencyName", "commandName", "duration", "success" };
				break;
			default:
				return; // nop; handled one layer above.
		}
		List<String> missing = new ArrayList<String>();
		for (String p : rp) {
			if (req.getParameter(p) == null) {
				missing.add(p);
			}
		}
		if (missing.size() > 0) {
			throw new IllegalArgumentException("missing required parameters: "+Arrays.toString(missing.toArray()));
		}
	}

	private boolean validateOptionalParameters(String path, HttpServletRequest req) {
		String op[] = null;
		switch (path) {
			case "metric":
				op = new String[] { "min", "max", "sampleCount" };
				break;
			case "event":
				break;
			case "httprequest":
			case "dependency":
			case "trace":
				op = new String[0];
				break;
			case "exception":
				break;
			case "request":
				break;
			default:
				throw new IllegalStateException("this shouldn't happen: unknown path element in optional parameter check");
		}
		List<String> missing = new ArrayList<String>();
		boolean foundOne = false;
		for (String p : op) {
			if (req.getParameter(p) == null) {
				missing.add(p);
			} else {
				foundOne = true;
			}
		}
		if (!("httprequest".equals(path) && "request".equals(path))) {
			foundOne |= req.getParameter("properties") != null; // properties is always optional but requires the other optional params
		}
		if (foundOne && missing.size() > 0) {
			throw new IllegalArgumentException("missing parameters in optional group: "+Arrays.toString(missing.toArray()));
		}
		return foundOne;
	}

	private void initVars() {
		if (tc == null || cases == null) {
			synchronized (lock) {
				if (tc == null) tc = new TelemetryClient();
				if (cases == null) cases = new AiTestCases(tc);
			}
		}
	}
}