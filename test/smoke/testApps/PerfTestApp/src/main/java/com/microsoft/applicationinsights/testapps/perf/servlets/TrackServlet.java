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

//	@GuardedBy("lock")
	private volatile TelemetryClient tc;

//	@GuardedBy("lock")
	private volatile FixedAiTestCases cases;

	private static final Object lock = new Object();

	private final Map<String, TestCaseRunnableFactory> factoryMap = new HashMap<String, TestCaseRunnableFactory>(){{
		put("metric/helper/measurement", new TestCaseRunnableFactory("trackMetric") {
			@Override
			public Runnable getRunnable() {
				return cases.getTrackMetric_HelperMeasurement();
			}
		});
		put("metric/helper/aggregate", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackMetric_HelperAggregate();
			}
		});
		put("metric/full/aggregate", new TestCaseRunnableFactory("trackMetric_Aggregate") {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackMetric_FullAggregate();
			}
		});
		put("metric/full/measurement", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackMetric_FullMeasurement();
			}
		});
		put("event", new TestCaseRunnableFactory("trackEvent") {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackEvent();
			}
		});
		put("httpRequest", new TestCaseRunnableFactory("trackHttpRequest") {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackHttpRequest_Success();
			}
		});
		put("request/full", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackRequest_Full();
			}
		});
		put("dependency", new TestCaseRunnableFactory("trackDependency") {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackDependency();
			}
		});
		put("dependency/full", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackDependency_Full();
			}
		});
		put("trace", new TestCaseRunnableFactory("trackTrace") {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackTrace();
			}
		});
		put("exception", new TestCaseRunnableFactory("trackException") {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackException();
			}
		});
		put("pageView", new TestCaseRunnableFactory("trackPageView") {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackPageView();
			}
		});
		put("pageView/full", new TestCaseRunnableFactory() {
			@Override
			protected Runnable getRunnable() {
				return cases.getTrackPageView_Full();
			}
		});
	}};

	@Override
	protected void reallyDoGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		initVars();
		String path = req.getPathInfo();
		System.out.println("GET "+path);
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
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