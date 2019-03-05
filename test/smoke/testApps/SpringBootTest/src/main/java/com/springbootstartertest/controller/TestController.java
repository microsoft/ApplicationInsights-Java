package com.springbootstartertest.controller;

import com.microsoft.applicationinsights.TelemetryClient;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	@Autowired
	TelemetryClient client;

	private CloseableHttpClient httpClient = HttpClients.createDefault();

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
		throw new Exception("This is an exception");
	}

	@Async
	@GetMapping("/asyncDependencyCall")
	public CompletableFuture<Integer> asyncDependencyCall() throws IOException {
        String url = "https://www.google.com";
        HttpGet get = new HttpGet(url);
        try (CloseableHttpResponse response = httpClient.execute(get)){
            return CompletableFuture.completedFuture(response.getStatusLine().getStatusCode());
        }
	}
}
