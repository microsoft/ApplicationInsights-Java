package com.microsoft.applicationinsights.smoketestapp;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private final Counter counter = Metrics.counter("test_counter");

    @GetMapping("/")
    public String root() {
        return "OK";
    }

    @GetMapping("/test")
    public String test() {
        counter.increment();
        return "OK!";
    }
}
