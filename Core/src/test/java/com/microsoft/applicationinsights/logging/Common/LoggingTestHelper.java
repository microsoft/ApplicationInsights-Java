package com.microsoft.applicationinsights.logging.Common;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import java.util.List;
import com.microsoft.applicationinsights.channel.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;

/**
 * Provides helper methods for tests.
 */
public class LoggingTestHelper {

    /**
     * Constructs new telemetry channel mock that adds sent telemetries to the given telemetry collection.
     * @param telemetryCollection
     * @return Telemetry channel mock that adds sent telemetries to the given collection.
     */
    public static TelemetryChannel createMockTelemetryChannelWithGivenTelemetryCollection(List<Telemetry> telemetryCollection) {
        TelemetryChannel telemetryChannelMock = mock(TelemetryChannel.class);
        doAnswer(
                invocation -> {
                    Telemetry telemetry = ((Telemetry) invocation.getArguments()[0]);
                    telemetryCollection.add(telemetry);

                    return null;
                }
        ).when(telemetryChannelMock).send(any(Telemetry.class));

        return telemetryChannelMock;
    }
}
