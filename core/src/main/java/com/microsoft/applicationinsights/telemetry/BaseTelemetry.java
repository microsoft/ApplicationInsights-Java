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

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import com.google.common.base.Charsets;
import com.microsoft.applicationinsights.internal.schemav2.Data;
import com.microsoft.applicationinsights.internal.schemav2.Domain;
import com.microsoft.applicationinsights.internal.schemav2.Envelope;
import com.microsoft.applicationinsights.internal.util.LocalStringsUtils;
import com.squareup.moshi.JsonWriter;
import okio.Buffer;
import org.apache.commons.lang3.StringUtils;

/**
 * Superclass for all telemetry data classes.
 */
public abstract class BaseTelemetry<T extends Domain> implements Telemetry {
    private TelemetryContext context;
    private Date timestamp;
    private String telemetryName;

    // this is temporary until we are convinced that telemetry are never re-used by codeless agent
    private volatile boolean used;

    public static final String TELEMETRY_NAME_PREFIX = "Microsoft.ApplicationInsights.";

    private static final ThreadLocal<DateFormat> dateFormat = new ThreadLocal<DateFormat>() {
        protected DateFormat initialValue() {
            return LocalStringsUtils.getDateFormatter();
        }
    };

    protected BaseTelemetry() {
    }

    /**
     * Initializes the instance with the context properties
     *
     * @param properties The context properties
     */
    protected void initialize(ConcurrentMap<String, String> properties) {
        this.context = new TelemetryContext(properties, new ContextTagsMap());
    }

    public void setTelemetryName(String telemetryName) {
        this.telemetryName = telemetryName;
    }

    /**
     * Gets date and time when event was recorded.
     *
     * @return The timestamp as Date
     */
    @Override
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Sets date and time when event was recorded.
     *
     * @param date The timestamp as Date.
     */
    @Override
    public void setTimestamp(Date date) {
        timestamp = date;
    }

    /**
     * Gets the context associated with the current telemetry item.
     *
     * @return The context
     */
    @Override
    public TelemetryContext getContext() {
        return context;
    }

    /**
     * Gets a dictionary of application-defined property names and values providing additional information about this event.
     *
     * @return The properties
     */
    @Override
    public Map<String, String> getProperties() {
        return this.context.getProperties();
    }

    /**
     * Serializes this object in JSON format.
     *
     * @param writer The writer that helps with serializing into Json format
     * @throws IOException The exception that might be thrown during the serialization
     */
    @Override
    public void serialize(JsonTelemetryDataSerializer writer) throws IOException {

        if (telemetryName == null || telemetryName.isEmpty()) {
            telemetryName = getTelemetryName(context.getNormalizedInstrumentationKey(), this.getEnvelopName());
        }

        Envelope envelope = new Envelope();
        envelope.setName(telemetryName);

        setSampleRate(envelope);
        envelope.setIKey(context.getInstrumentationKey());
        Data<T> tmp = new Data<>();
        tmp.setBaseData(getData());
        tmp.setBaseType(this.getBaseTypeName());
        envelope.setData(tmp);
        if (getTimestamp() != null) envelope.setTime(dateFormat.get().format(getTimestamp()));
        envelope.setTags(context.getTags());

        envelope.serialize(writer);
    }

    /**
     * THIS IS FOR DEBUGGING AND TESTING ONLY!
     * DON'T USE THIS IN HAPPY-PATH, PRODUCTION CODE.
     *
     * @return Json representation of this telemetry item.
     */
    @Override
    public String toString() {
        Buffer buffer = new Buffer();
        try {
            JsonWriter jw = JsonWriter.of(buffer);
            JsonTelemetryDataSerializer jtds = new JsonTelemetryDataSerializer(jw);
            this.serialize(jtds);
            jtds.close();
            jw.close();
            return new String(buffer.readByteArray(), Charsets.UTF_8);
        } catch (IOException e) {
            // shouldn't happen with a string writer
            throw new RuntimeException("Error serializing "+this.getClass().getSimpleName()+" toString", e);
        }
    }

    // this is temporary until we are convinced that telemetry are never re-used by codeless agent
    public boolean previouslyUsed() {
        return used;
    }

    @Override
    // this is temporary until we are convinced that telemetry are never re-used by codeless agent
    public void markUsed() {
        used = true;
    }

    /**
     * Concrete classes should implement this method which supplies the
     * data structure that this instance works with, which needs to implement {@link JsonSerializable}
     *
     * @return The inner data structure
     */
    protected abstract T getData();

    protected void setSampleRate(Envelope envelope) {
    }

    public String getEnvelopName() {
        throw new UnsupportedOperationException();
    }

    public String getBaseTypeName() {
        throw new UnsupportedOperationException();
    }

    public static String normalizeInstrumentationKey(String instrumentationKey){
        if (StringUtils.isEmpty(instrumentationKey) || StringUtils.containsOnly(instrumentationKey, ".- ")){
            return "";
        }
        else{
            return instrumentationKey.replace("-", "").toLowerCase() + ".";
        }
    }

    public static String getTelemetryName(String normalizedInstrumentationKey, String envelopType){
        return TELEMETRY_NAME_PREFIX + normalizedInstrumentationKey + envelopType;
    }

}
