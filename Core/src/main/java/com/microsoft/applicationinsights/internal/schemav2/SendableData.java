package com.microsoft.applicationinsights.internal.schemav2;

import com.microsoft.applicationinsights.telemetry.JsonSerializable;

/**
 * Created by gupele on 12/25/2014.
 */
public interface SendableData extends JsonSerializable {
    String getEnvelopName();

    String getBaseTypeName();
}
