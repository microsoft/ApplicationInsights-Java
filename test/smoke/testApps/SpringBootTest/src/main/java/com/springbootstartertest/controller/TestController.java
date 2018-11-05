package com.springbootstartertest.controller;

import com.microsoft.applicationinsights.TelemetryClient;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

	@Autowired
	TelemetryClient client;

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
}
