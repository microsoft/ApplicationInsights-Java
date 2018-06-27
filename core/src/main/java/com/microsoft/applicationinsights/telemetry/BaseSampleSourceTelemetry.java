package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;

/** Created by gupele on 12/4/2016. */
public abstract class BaseSampleSourceTelemetry<T extends Domain> extends BaseTelemetry<T>
    implements SupportSampling {

  @Override
  public void reset() {
    setSamplingPercentage(null);
  }

  @Override
  protected void setSampleRate(Envelope envelope) {
    Double currentSP = getSamplingPercentage();
    if (currentSP != null) {
      envelope.setSampleRate(currentSP);
    }
  }
}
