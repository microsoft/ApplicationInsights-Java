package com.microsoft.applicationinsights.channel;

import java.util.Collection;
import com.google.common.base.Optional;

/**
 * Created by gupele on 12/17/2014.
 *
 * An interface for serializing container of telemetries
 * Concrete classes should be able to create a compressed byte array
 * that represents at collection of Telemetry instances
 */
public interface TelemetrySerializer {
    /**
     *
     * @param telemetries A collection of Telemetry instances
     * @return byte array that is a compressed version of the input
     */
    Optional<Transmission> serialize(Collection<Telemetry> telemetries);
}
