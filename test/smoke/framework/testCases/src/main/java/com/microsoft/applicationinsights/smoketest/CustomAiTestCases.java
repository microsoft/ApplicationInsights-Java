package com.microsoft.applicationinsights.smoketest;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.Duration;
import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;

import java.util.Date;
import java.util.Map;

public class CustomAiTestCases {
	public final TelemetryClient tclient;

	public CustomAiTestCases(TelemetryClient tclient) {
		this.tclient = tclient;
	}

	public Runnable getTrackEvent(final String name, final Map<String, String> properties, final Map<String, Double> metrics) {
		return new Runnable() {
			@Override
			public void run() {
				tclient.trackEvent(name, properties, metrics);
			}
		};
	}

	public Runnable getTrackTrace(final String message, final SeverityLevel severityLevel, Map<String, String> properties) {
		return new Runnable() {
			@Override
			public void run() {
				tclient.trackTrace(message, severityLevel, properties);
			}
		};
	}

	public Runnable getTrackMetric(final String name, final double value, final int sampleCount, final double min, final double max, Map<String, String> properties) {
		return new Runnable(){
			@Override
			public void run() {
				tclient.trackMetric(name, value, sampleCount, min, max, properties);
			}
		};
	}

	public Runnable getTrackMetric(final String name, final double value) {
		return new Runnable(){
			@Override
			public void run() {
				tclient.trackMetric(name, value);				
			}
		};
	}

	public Runnable getTrackException(final Exception exception, Map<String, String> properties, Map<String, Double> metrics) {
		return new Runnable(){
			@Override
			public void run() {
				tclient.trackException(exception, properties, metrics);
			}
		};
	}

	public Runnable getTrackHttpRequest(final String name, final Date timestamp, final long duration, final String responseCode, final boolean success) {
		return new Runnable(){
			@Override
			public void run() {
				tclient.trackHttpRequest(name, timestamp, duration, responseCode, success);
			}
		};
	}

	public Runnable getTrackRequest(final RequestTelemetry telemetry) {
		return new Runnable(){
			@Override
			public void run() {
				tclient.trackRequest(telemetry);
			}
		};
	}

	public Runnable getTrackDependency(final String dependencyName, final String commandName, final Duration duration, final boolean success) {
		return new Runnable(){
			@Override
			public void run() {
				tclient.trackDependency(dependencyName, commandName, duration, success);
			}
		};
	}

	public Runnable getTrackPageView(final String name) {
		return new Runnable(){
			@Override
			public void run() {
				tclient.trackPageView(name);
			}
		};
	}
	
}