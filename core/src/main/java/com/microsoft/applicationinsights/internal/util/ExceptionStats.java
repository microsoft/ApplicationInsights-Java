package com.microsoft.applicationinsights.internal.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import com.microsoft.applicationinsights.internal.channel.common.TransmissionFileSystemOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// exception stats for a given 5-min window
// each instance represents a logical grouping of errors that a user cares about and can understand,
// e.g. sending telemetry to the portal, storing telemetry to disk, ...
public class ExceptionStats {

    private static final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(ExceptionStats.class, "exception stats logger"));

    private static final Logger logger = LoggerFactory.getLogger(ExceptionStats.class);

    // Period for scheduled executor in secs
    private final int intervalSeconds;

    private final AtomicBoolean firstFailure = new AtomicBoolean();

    // private final String groupingMessage;

    // number of successes and failures in the 5-min window
    private long numSuccesses;
    private long numFailures;

    // using MutableLong for two purposes
    // * so we don't need to get and set into map each time we want to increment
    // * avoid autoboxing for values above 128
    private Map<String, MutableLong> warningMessages = new HashMap<>();

    private final Object lock = new Object();

    public ExceptionStats() {
        this(300);
    }

    // Primarily used by test
    public ExceptionStats(int intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public void recordSuccess() {
        synchronized (lock) {
            numSuccesses++;
        }
    }

    // warningMessage should have low cardinality
    public void recordException(String warningMessage, Exception exception, Logger logger) {
        if (!firstFailure.getAndSet(true)) {
            // log the first time we see an exception as soon as it occurs
            logger.warn(warningMessage + " (future failures will be aggregated and logged once every "+ this.intervalSeconds /60 +" minutes)", exception);
            scheduledExecutor.scheduleAtFixedRate(new ExceptionStatsLogger(), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            return;
        }

        logger.debug(warningMessage, exception);

        synchronized (lock) {
            warningMessages.computeIfAbsent(warningMessage, key -> new MutableLong()).increment();
            numFailures++;
        }
    }

    public void recordError(String warningMessage, Logger logger) {
        if (!firstFailure.getAndSet(true)) {
            // log the first time we see an exception as soon as it occurs
            logger.warn(warningMessage + " (future failures will be aggregated and logged once every "+ this.intervalSeconds /60 +" minutes)");
            scheduledExecutor.scheduleAtFixedRate(new ExceptionStatsLogger(), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            return;
        }

        logger.debug(warningMessage);

        synchronized (lock) {
            warningMessages.computeIfAbsent(warningMessage, key -> new MutableLong()).increment();
            numFailures++;
        }
    }

    private static class MutableLong {
        private long value;
        private void increment() {
            value++;
        }
    }

    public class ExceptionStatsLogger implements Runnable {

        @Override
        public void run() {
            long numSuccesses;
            long numFailures;
            Map<String, MutableLong> warningMessages;
            // grab quickly and reset under lock (do not perform logging under lock)
            synchronized (lock) {
                numSuccesses = ExceptionStats.this.numSuccesses;
                numFailures = ExceptionStats.this.numFailures;
                warningMessages = ExceptionStats.this.warningMessages;

                ExceptionStats.this.numSuccesses = 0;
                ExceptionStats.this.numFailures = 0;
                ExceptionStats.this.warningMessages = new HashMap<>();
            }
            if (numFailures > 0) {
                warningMessages.forEach(
                        (message,failureCount) -> logger.error(message+" (failed "+ failureCount.value + " times in the last "+ ExceptionStats.this.intervalSeconds/60 +" minutes)")
                );
                logger.warn(numFailures+"/"+(numFailures+numSuccesses) + "(Total Failures/Total Requests) reported in the last "+ ExceptionStats.this.intervalSeconds/60 +" minutes");

            }
        }
    }
}
