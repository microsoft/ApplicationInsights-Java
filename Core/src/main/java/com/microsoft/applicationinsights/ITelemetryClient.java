/**
 * Temporary interface - please ignore since this class will not be checked in by me.
 * Currently we don't have an interface for the Telemetry client and so I implemented this thin
 * interface in order to unblock to UTs.
 *
 * gupele is about to submit a formal interface that will be merged into my change before my submit.
 */

package com.microsoft.applicationinsights;

import com.microsoft.applicationinsights.channel.Telemetry;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.datacontracts.TelemetryContext;

public interface ITelemetryClient {
    void track(Telemetry telemetry);
    TelemetryContext getContext();
    void setChannel(TelemetryChannel telemetryChannel);
    TelemetryChannel getChannel();
}
