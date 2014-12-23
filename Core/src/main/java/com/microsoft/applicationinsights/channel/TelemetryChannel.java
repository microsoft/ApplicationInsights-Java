package com.microsoft.applicationinsights.channel;

import com.microsoft.applicationinsights.channel.contracts.Data;
import com.microsoft.applicationinsights.channel.contracts.Envelope;
import com.microsoft.applicationinsights.channel.contracts.shared.ITelemetry;
import com.microsoft.applicationinsights.channel.contracts.shared.ITelemetryData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * This class records telemetry for application insights.
 */
public class TelemetryChannel {
    /**
     * TAG for log cat.
     */
    private static final String TAG = "TelemetryChannel";

    /**
     * The configuration for this recorder
     */
    private final IChannelConfig config;

    /**
     * Test hook to the sender
     */
    protected Sender sender;

    /**
     * Instantiates a new instance of Sender
     * @param config The configuration for this channel
     */
    public TelemetryChannel(IChannelConfig config) {
        this.sender = Sender.instance;
        this.config = config;
    }

    /**
     * Constructor for tests
     * @param config The configuration for this channel
     * @param sender The sender for this channel
     */
    protected TelemetryChannel(IChannelConfig config, Sender sender) {
        this.sender = sender;
        this.config = config;
    }

    /**
     * Records the passed in data.
     *
     * @param telemetryContext The telemetry telemetryContext for this record
     * @param telemetry The telemetry to record
     * @param envelopeName Value to fill Envelope's Content
     * @param baseType Value to fill Envelope's ItemType field
     */
    public void send(ITelemetryContext telemetryContext,
                       ITelemetry telemetry,
                       String envelopeName,
                       String baseType) {

        // add common properties to this telemetry object
        HashMap<String, String> map = telemetry.getProperties();
        map.putAll(telemetryContext.getProperties());
        telemetry.setProperties(map);

        // wrap the telemetry data in the common schema data
        Data<ITelemetryData> data = new Data<ITelemetryData>();
        data.setBaseData(telemetry);
        data.setBaseType(baseType);

        // wrap the data in the common schema envelope
        Envelope envelope = new Envelope();
        envelope.setIKey(this.config.getInstrumentationKey());
        envelope.setData(data);
        envelope.setName(envelopeName);
        envelope.setTime(this.getUtcTime());
        envelope.setTags(telemetryContext.toHashMap());

        // send to queue
        this.sender.queue(envelope);
    }

    /**
     * Gets a UTC formatted time stamp.
     * @return UTC format string representing the current time.
     */
    private String getUtcTime() {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(new Date());
    }
}
