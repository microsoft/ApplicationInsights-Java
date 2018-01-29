package com.microsoft.applicationinsights.testapps.perf.servlets;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.smoketest.FixedAiTestCases;
import com.microsoft.applicationinsights.testapps.perf.TestCaseRunnableFactory;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = "/track/*")
public class TrackServlet extends APerfTestServlet {
	public static final long serialVersionUID = -1L;
	
	private volatile TelemetryClient tc;
	private volatile FixedAiTestCases cases;
	private static final Object lock = new Object();

	private final Map<String, TestCaseRunnableFactory> factoryMap = new HashMap<String, TestCaseRunnableFactory>(){{
		put("metric", new TestCaseRunnableFactory() {
			@Override
			public Runnable getRunnable() {
				return cases.getTrackMetric();
			}
		});
		put("event", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackEvent();
			}
		});
		put("httpRequest", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackHttpRequest();
			}
		});
		put("dependency", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackDependency();
			}
		});
		put("trace", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackTrace();
			}
		});
		put("exception", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackException();
			}
		});
		put("pageView", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackPageView();
			}
		});
	}};

	@Override
	protected void reallyDoGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		initVars();
		String path = req.getPathInfo();
		TestCaseRunnableFactory f = factoryMap.get(path);
		if (f == null) {
			resp.sendError(404, "Unknown path: "+path);
			return;
		}
		f.get().run();
	}

	private void initVars() {
		if (tc == null || cases == null) {
			synchronized (lock) {
				if (tc == null) tc = new TelemetryClient();
				if (cases == null) cases = new FixedAiTestCases(tc);
			}
		}
	}
}