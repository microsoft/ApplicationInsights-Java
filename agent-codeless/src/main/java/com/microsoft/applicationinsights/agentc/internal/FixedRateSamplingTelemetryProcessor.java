package com.microsoft.applicationinsights.agentc.internal;

import java.util.Map;

import com.microsoft.applicationinsights.extensibility.TelemetryProcessor;
import com.microsoft.applicationinsights.internal.channel.samplingV2.SamplingScoreGeneratorV2;
import com.microsoft.applicationinsights.telemetry.SupportSampling;
import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FixedRateSamplingTelemetryProcessor implements TelemetryProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FixedRateSamplingTelemetryProcessor.class);

    // all sampling percentage must be in a ratio of 100/N where N is a whole number (2, 3, 4, …)
    // e.g. 50 for 1/2 or 33.33 for 1/3
    //
    // failure to follow this pattern can result in unexpected / incorrect computation of values in the portal
    private double samplingPercentage;

    private final Map<Class<?>, Double> samplingPercentages;

    public FixedRateSamplingTelemetryProcessor(double samplingPercentage, Map<Class<?>, Double> samplingPercentages) {
        this.samplingPercentage = samplingPercentage;
        this.samplingPercentages = samplingPercentages;
    }

    @Override
    public boolean process(Telemetry telemetry) {
        if (!(telemetry instanceof SupportSampling)) {
            return true;
        }
        Double sp = samplingPercentages.get(telemetry.getClass());
        if (sp == null) {
            sp = samplingPercentage;
        }
        if (sp == 100) {
            return true;
        }
        ((SupportSampling) telemetry).setSamplingPercentage(sp);
        if (SamplingScoreGeneratorV2.getSamplingScore(telemetry) >= sp) {
            logger.debug("Item {} sampled out", telemetry.getClass().getSimpleName());
            return false;
        }
        return true;
    }
}
