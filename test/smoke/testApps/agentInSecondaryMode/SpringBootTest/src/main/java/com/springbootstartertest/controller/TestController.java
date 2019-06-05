package com.springbootstartertest.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import javax.servlet.ServletException;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	private static final TelemetryClient client = new TelemetryClient();

	@Autowired
	private TestBean testBean;

	@GetMapping("/")
	public String rootPage() {
		return "OK";
	}

	@GetMapping("/basic/trackEvent")
	public String trackEventSpringBoot() {
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
		return "hello";
	}

	@GetMapping("/throwsException")
	public void resultCodeTest() throws Exception {
		throw new ServletException("This is an exception");
	}

	@GetMapping("/asyncDependencyCallWithApacheHttpClient4")
	public Future<Integer> asyncDependencyCallWithApacheHttpClient4() throws IOException {
		return testBean.asyncDependencyCallWithApacheHttpClient4();
	}

    @GetMapping("/asyncDependencyCallWithApacheHttpClient3")
    public Future<Integer> asyncDependencyCallWithApacheHttpClient3() throws IOException {
		return testBean.asyncDependencyCallWithApacheHttpClient3();
    }

    @GetMapping("/asyncDependencyCallWithOkHttp3")
    public Future<Integer> asyncDependencyCallWithOkHttp3() throws IOException {
		return testBean.asyncDependencyCallWithOkHttp3();
    }

	@GetMapping("/asyncDependencyCallWithOkHttp2")
	public Future<Integer> asyncDependencyCallWithOkHttp2() throws IOException {
		return testBean.asyncDependencyCallWithOkHttp2();
	}

	@GetMapping("/asyncDependencyCallWithHttpURLConnection")
	public Future<Integer> asyncDependencyCallWithHttpURLConnection() throws IOException {
		return testBean.asyncDependencyCallWithHttpURLConnection();
	}
}
