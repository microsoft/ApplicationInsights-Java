package com.microsoft.applicationinsights.testapps.perf.boot;

import com.google.common.base.Stopwatch;
import com.microsoft.applicationinsights.testapps.perf.TestCaseRunnable;

import java.util.concurrent.TimeUnit;

public class SpringBootPerfTestHelper {
    public static String runTest(TestCaseRunnable runnable) {
        Stopwatch sw = Stopwatch.createStarted();
        try {
            runnable.run();
        } finally {
            sw.stop();
            return String.valueOf(sw.elapsed(TimeUnit.MILLISECONDS));
        }
    }
}
