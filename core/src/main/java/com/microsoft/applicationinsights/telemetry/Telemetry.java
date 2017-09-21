/*
 * ApplicationInsights-Java
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * MIT License
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the ""Software""), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package com.microsoft.applicationinsights.telemetry;

import org.apache.http.annotation.Obsolete;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * The base telemetry type interface for application insights.
 */
public interface Telemetry extends JsonSerializable
{
    /**
     * Gets the time when telemetry was recorded
     * @return Recorded time.
     */
    Date getTimestamp();

    /**
     * Sequence field used to track absolute order of uploaded events.
     * It is a two-part value that includes a stable identifier for the current boot
     * session and an incrementing identifier for each event added to the upload queue
     *
     * The Sequence helps track how many events were fired and how many events were uploaded and
     * enables identification of data lost during upload and de-duplication of events on the ingress server.
     *
     * Gets the current sequence.
     * @return The current sequence.
     */
    String getSequence();

    /**
     * Sets the sequence.
     * @param sequence The sequence.
     */
    void setSequence(String sequence);

    /**
     * Sets the time when telemetry was recorded
     * @param date Recorded time.
     */
    void setTimestamp(Date date);

    /**
     * Gets the context associated with this telemetry instance.
     * @return Context associated with this instance.
     */
    TelemetryContext getContext();

    /**
     * Gets the map of application-defined property names and values.
     * @return Map of properties.
     */
    Map<String,String> getProperties();

    /**
     * @deprecated
     * Sanitizes the properties of the telemetry item based on DP constraints.
     */
    @Deprecated
    void sanitize();

    /**
     * Serializes itself to Json using the {@link JsonTelemetryDataSerializer}
     * @param writer The writer that helps with serializing into Json format
     * @throws IOException a possible exception
     */
    void serialize(JsonTelemetryDataSerializer writer) throws IOException;

    void reset();
}
