package com.microsoft.applicationinsights.telemetry;

import com.microsoft.applicationinsights.internal.schemav2.Domain;

/** Created by gupele on 12/4/2016. */
public abstract class BaseSampleSourceTelemetry<T extends Domain> extends BaseTelemetry<T>
    implements SupportSampling {}
