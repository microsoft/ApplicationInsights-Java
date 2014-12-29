package com.microsoft.applicationinsights.implementation.schemav2;

import com.microsoft.applicationinsights.datacontracts.JsonSerializable;

/**
 * Created by gupele on 12/25/2014.
 */
public interface SendableData extends JsonSerializable {
    String getEnvelopName();

    String getBaseTypeName();
}
