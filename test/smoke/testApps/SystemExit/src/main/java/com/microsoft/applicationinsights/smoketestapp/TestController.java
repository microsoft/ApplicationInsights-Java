package com.microsoft.applicationinsights.smoketestapp;

import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

@RestController
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/")
    public String root() {
        return "OK";
    }

    @GetMapping("/delayedSystemExit")
    public String delayedSystemExit() {
        // need a small delay to ensure response has been sent
        Executors.newScheduledThreadPool(1)
                .schedule(() -> {
                    logger.error("this is an error right before shutdown");
                    System.exit(0);
                }, 200, MILLISECONDS);
        return "OK!";
    }
}
