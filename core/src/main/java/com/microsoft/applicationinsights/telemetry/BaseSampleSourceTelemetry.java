package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.schemav2.SendableData;

/**
 * Created by gupele on 12/4/2016.
 */
public abstract class BaseSampleSourceTelemetry<T extends SendableData> extends BaseTelemetry<T> implements SupportSampling {

    @Override
    public void reset() {
        setSamplingPercentage(null);
    }


    @Override
    protected void setSampleRate(Envelope envelope) {
        Double currentSP = getSamplingPercentage();
        envelope.setSampleRate(currentSP);
    }
}
