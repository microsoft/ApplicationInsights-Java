package com.microsoft.applicationinsights.smoketestapp;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final Counter counter = Metrics.counter("test_counter");
    private final Counter excludedCounter = Metrics.counter("test_counter_exclude_me");
    private final Counter anotherExcludedCounter = Metrics.counter("exclude_me_test_counter");

    @GetMapping("/")
    public String root() {
        return "OK";
    }

    @GetMapping("/test")
    public String test() {
        excludedCounter.increment();
        anotherExcludedCounter.increment();
        counter.increment();
        return "OK!";
    }
}
