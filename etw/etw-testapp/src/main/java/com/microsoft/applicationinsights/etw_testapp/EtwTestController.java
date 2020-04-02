package com.microsoft.applicationinsights.etw_testapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EtwTestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EtwTestController.class);
    private static final Logger DIAGNOSTICS_LOGGER = LoggerFactory.getLogger("applicationinsights.extension.diagnostics");

    public EtwTestController() {
        info("Loading EtwTestController");
    }

    @GetMapping("/")
    public String rootPage() {
        info("Hit EtwTestController.rootPage");
        return "ROOT";
    }

    private static void info(String message) {
        LOGGER.info(message);
        DIAGNOSTICS_LOGGER.info(message);
    }

}