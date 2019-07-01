package com.springbootstartertest.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;

import com.microsoft.applicationinsights.TelemetryClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

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
    public DeferredResult<Integer> asyncDependencyCallWithApacheHttpClient4() throws IOException {
        DeferredResult<Integer> deferredResult = new DeferredResult<>();
        testBean.asyncDependencyCallWithApacheHttpClient4(deferredResult);
        return deferredResult;
    }

    @GetMapping("/asyncDependencyCallWithApacheHttpClient3")
    public DeferredResult<Integer> asyncDependencyCallWithApacheHttpClient3() throws IOException {
        DeferredResult<Integer> deferredResult = new DeferredResult<>();
        testBean.asyncDependencyCallWithApacheHttpClient3(deferredResult);
        return deferredResult;
    }

    @GetMapping("/asyncDependencyCallWithOkHttp3")
    public DeferredResult<Integer> asyncDependencyCallWithOkHttp3() throws IOException {
        DeferredResult<Integer> deferredResult = new DeferredResult<>();
        testBean.asyncDependencyCallWithOkHttp3(deferredResult);
        return deferredResult;
    }

    @GetMapping("/asyncDependencyCallWithOkHttp2")
    public DeferredResult<Integer> asyncDependencyCallWithOkHttp2() throws IOException {
        DeferredResult<Integer> deferredResult = new DeferredResult<>();
        testBean.asyncDependencyCallWithOkHttp2(deferredResult);
        return deferredResult;
    }

    @GetMapping("/asyncDependencyCallWithHttpURLConnection")
    public DeferredResult<Integer> asyncDependencyCallWithHttpURLConnection() throws IOException {
        DeferredResult<Integer> deferredResult = new DeferredResult<>();
         testBean.asyncDependencyCallWithHttpURLConnection(deferredResult);
        return deferredResult;
    }
}
