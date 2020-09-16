package com.microsoft.applicationinsights.smoketest;

import com.google.common.base.Stopwatch;
import org.junit.*;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

@UseAgent("FixedRateSampling")
public class FixedRateSamplingTest extends AiSmokeTest {

    @Test
    @TargetUri(value = "/fixedRateSampling", callCount = 100)
    public void testFixedRateSamplingInIncludedTypes() throws Exception {
        // super super low chance that number of sampled requests is less than 10
        Stopwatch stopwatch = Stopwatch.createStarted();
        while (mockedIngestion.getCountForType("RequestData") < 10 && stopwatch.elapsed(SECONDS) < 10) {
        }
        // wait ten more seconds to before checking that we didn't receive too many
        Thread.sleep(SECONDS.toMillis(10));
        int requestCount = mockedIngestion.getCountForType("RequestData");
        int eventCount = mockedIngestion.getCountForType("EventData");
        // super super low chance that number of sampled requests/events is greater than 90
        assertThat(requestCount, lessThanOrEqualTo(90));
        assertThat(eventCount, lessThanOrEqualTo(90));
    }
}
