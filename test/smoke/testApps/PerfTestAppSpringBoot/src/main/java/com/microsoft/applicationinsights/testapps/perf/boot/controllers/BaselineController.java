package com.microsoft.applicationinsights.testapps.perf.boot.controllers;

import com.microsoft.applicationinsights.testapps.perf.TestCaseRunnable;
import com.microsoft.applicationinsights.testapps.perf.boot.SpringBootPerfTestHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BaselineController {
    @GetMapping("/baseline")
    public String baseline() {
        return SpringBootPerfTestHelper.runTest(new TestCaseRunnable(null));
    }
}
