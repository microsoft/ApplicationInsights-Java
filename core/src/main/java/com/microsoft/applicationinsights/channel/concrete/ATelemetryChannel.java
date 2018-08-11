package com.microsoft.applicationinsights.channel.concrete;

import com.google.common.base.Preconditions;
import com.microsoft.applicationinsights.channel.TelemetryChannel;
import com.microsoft.applicationinsights.channel.TelemetrySampler;
import com.microsoft.applicationinsights.internal.channel.TelemetriesTransmitter;
import com.microsoft.applicationinsights.internal.channel.TransmitterFactory;
import com.microsoft.applicationinsights.internal.channel.common.TelemetryBuffer;
import com.microsoft.applicationinsights.internal.logger.InternalLogger;

import com.microsoft.applicationinsights.telemetry.Telemetry;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// FIXME better name?
public abstract class ATelemetryChannel<T> implements TelemetryChannel {
    public static final int DEFAULT_MAX_INSTANT_RETRY = 3;
    public static final int DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY = 500;
    public static final int DEFAULT_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 5;
    public static final int MIN_MAX_TELEMETRY_BUFFER_CAPACITY = 1;
    public static final int MAX_MAX_TELEMETRY_BUFFER_CAPACITY = 1000;
    public static final int MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 1;
    public static final int MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS = 300;
    public static final String DEVELOPER_MODE_SYSTEM_PROPRETY_NAME = "APPLICATION_INSIGHTS_DEVELOPER_MODE";

    protected static final String MAX_MAX_TELEMETRY_BUFFER_CAPACITY_NAME = "MaxTelemetryBufferCapacity";
    protected static final String INSTANT_RETRY_NAME = "MaxInstantRetry";
    protected static final String FLUSH_BUFFER_TIMEOUT_IN_SECONDS_NAME = "FlushIntervalInSeconds";
    protected static final String DEVELOPER_MODE_NAME = "DeveloperMode";
    protected static final String ENDPOINT_ADDRESS_NAME = "EndpointAddress";
    protected static final String MAX_TRANSMISSION_STORAGE_CAPACITY_NAME = "MaxTransmissionStorageFilesCapacityInMB";
    protected static final int LOG_TELEMETRY_ITEMS_FACTOR = 10000;

    // FIXME why was this static???
    protected TransmitterFactory<T> s_transmitterFactory;
    protected static AtomicLong itemsSent = new AtomicLong(0);

    protected boolean stopped = false;

    protected TelemetriesTransmitter<T> telemetriesTransmitter;
    protected TelemetrySampler telemetrySampler;
    protected TelemetryBuffer<T> telemetryBuffer;

    private boolean developerMode = false;

    /**
     * Gets value indicating whether this channel is in developer mode.
     */
    @Override
    public boolean isDeveloperMode() {
        return developerMode;
    }

    /**
     * Sets value indicating whether this channel is in developer mode.
     *
     * If true, this also forces maxTelemetriesInBatch to be 1.
     *
	 * @param developerMode true or false
     */
    @Override
    public void setDeveloperMode(boolean developerMode) {
        if (developerMode != this.developerMode) {
            this.developerMode = developerMode;
            int maxTelemetriesInBatch = this.developerMode ? 1 : DEFAULT_MAX_TELEMETRY_BUFFER_CAPACITY;

            setMaxTelemetriesInBatch(maxTelemetriesInBatch);
        }
    }

    /**
     * Stops on going work
     */
    @Override
    public synchronized void stop(long timeout, TimeUnit timeUnit) {
        try {
            if (stopped) {
                return;
            }

            telemetriesTransmitter.stop(timeout, timeUnit);
            stopped = true;
        } catch (ThreadDeath td) {
        	throw td;
        } catch (Throwable t) {
            try {
                InternalLogger.INSTANCE.error("Exception generated while stopping telemetry transmitter");
                InternalLogger.INSTANCE.trace("Stack trace generated is %s", ExceptionUtils.getStackTrace(t));
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable t2) {
                // chomp
            }
        }
    }

    /**
     * Sets the time tow wait before flushing the internal buffer
     *
	 * @param transmitBufferTimeoutInSeconds
	 *            should be between MIN_FLUSH_BUFFER_TIMEOUT_IN_SECONDS and
	 *            MAX_FLUSH_BUFFER_TIMEOUT_IN_SECONDS inclusive if the number is
	 *            lower than the minimum then the minimum will be used if the number
	 *            is higher than the maximum then the maximum will be used
     */
    public void setTransmitBufferTimeoutInSeconds(int transmitBufferTimeoutInSeconds) {
        telemetryBuffer.setTransmitBufferTimeoutInSeconds(transmitBufferTimeoutInSeconds);
    }

    /**
     * Sets the buffer size
     *
	 * @param maxTelemetriesInBatch
	 *            should be between MIN_MAX_TELEMETRY_BUFFER_CAPACITY and
	 *            MAX_MAX_TELEMETRY_BUFFER_CAPACITY inclusive if the number is lower
	 *            than the minimum then the minimum will be used if the number is
	 *            higher than the maximum then the maximum will be used
     */
    public void setMaxTelemetriesInBatch(int maxTelemetriesInBatch) {
        telemetryBuffer.setMaxTelemetriesInBatch(maxTelemetriesInBatch);
    }

    /**
     * Flushes the data that the channel might have internally.
     */
    @Override
    public void flush() {
        telemetryBuffer.flush();
    }

    /**
	 * Sets an optional Sampler that can sample out telemetries Currently, we don't
	 * allow to replace a valid telemtry sampler.
     *
	 * @param telemetrySampler
	 *            - The sampler
     */
    @Override
    public void setSampler(TelemetrySampler telemetrySampler) {
        if (this.telemetrySampler == null) {
            this.telemetrySampler = telemetrySampler;
        }
    }

    /**
     * Sends a Telemetry instance through the channel.
     */
    @Override
    public void send(Telemetry telemetry) {
        Preconditions.checkNotNull(telemetry, "Telemetry item must be non null");

        if (isDeveloperMode()) {
            telemetry.getContext().getProperties().put("DeveloperMode", "true");
        }

        if (telemetrySampler != null) {
            if (!telemetrySampler.isSampledIn(telemetry)) {
                return;
            }
        }

        if (!doSend(telemetry)) {
            return;
        }

        if (itemsSent.incrementAndGet() % LOG_TELEMETRY_ITEMS_FACTOR == 0) {
            InternalLogger.INSTANCE.info("items sent till now %d", itemsSent.get());
        }

        if (isDeveloperMode()) {
            writeTelemetryToDebugOutput(telemetry);
        }
    }

    /**
     *
     * @param telemetry
     * @return true, if the send was successful, false if there was an error
     */
    protected abstract boolean doSend(Telemetry telemetry);

    private void writeTelemetryToDebugOutput(Telemetry telemetry) {
        InternalLogger.INSTANCE.trace("%s sending telemetry: %s", this.getClass().getSimpleName(), telemetry.toString());
    }
}
