package com.microsoft.applicationinsights.smoketest;

import com.google.common.base.Stopwatch;
import org.junit.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@UseAgent("sampling")
public class SamplingTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/sampling", callCount = 100)
    public void testSampling() throws Exception {
        // super super low chance that number of sampled requests is less than 25
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (mockedIngestion.getCountForType("RequestData") < 25 && stopwatch.elapsed(SECONDS) < 10) {
        }
        // wait ten more seconds to before checking that we didn't receive too many
        Thread.sleep(SECONDS.toMillis(10));
        int requestCount = mockedIngestion.getCountForType("RequestData");
        int eventCount = mockedIngestion.getCountForType("EventData");
        // super super low chance that number of sampled requests/events
        // is less than 25 or greater than 75
        assertThat(requestCount, greaterThanOrEqualTo(25));
        assertThat(eventCount, greaterThanOrEqualTo(25));
        assertThat(requestCount, lessThanOrEqualTo(75));
        assertThat(eventCount, lessThanOrEqualTo(75));
    }
}
