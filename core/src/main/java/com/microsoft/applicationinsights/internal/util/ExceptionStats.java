package com.microsoft.applicationinsights.internal.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// exception stats for a given 5-min window
// each instance represents a logical grouping of errors that a user cares about and can understand,
// e.g. sending telemetry to the portal, storing telemetry to disk, ...
public class ExceptionStats {

    private static final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(ExceptionStats.class, "exception stats logger"));

    private final Logger logger;
    private final String introMessage;

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

    public ExceptionStats(Class<?> source, String introMessage) {
        this(source, introMessage, 300);
    }

    // Primarily used by test
    public ExceptionStats(Class<?> source, String introMessage, int intervalSeconds) {
        logger = LoggerFactory.getLogger(source);
        this.introMessage = introMessage;
        this.intervalSeconds = intervalSeconds;
    }

    public void recordSuccess() {
        synchronized (lock) {
            numSuccesses++;
        }
    }

    // warningMessage should have low cardinality
    public void recordFailure(String warningMessage) {
        recordFailure(warningMessage, null);
    }

    // warningMessage should have low cardinality
    public void recordFailure(String warningMessage, @Nullable Throwable exception) {
        if (!firstFailure.getAndSet(true)) {
            // log the first time we see an exception as soon as it occurs, along with full stack trace
            logger.warn(introMessage + " " + warningMessage + " (future failures will be aggregated and logged once every " + intervalSeconds / 60 + " minutes)", exception);
            scheduledExecutor.scheduleAtFixedRate(new ExceptionStatsLogger(), intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
            return;
        }

        logger.debug(introMessage + " " + warningMessage, exception);

        if (warningMessages.size() > 100) {
            // we have a cardinality problem and don't want to consume too much memory or do too much logging
            return;
        }
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
                long numMinutes = ExceptionStats.this.intervalSeconds / 60;
                long total = numSuccesses + numFailures;
                StringBuilder message = new StringBuilder();
                message.append("In the last ");
                message.append(numMinutes);
                message.append(" minutes, the following operation has failed ");
                message.append(numFailures);
                message.append(" times (out of ");
                message.append(total);
                message.append("):\n");
                message.append(introMessage);
                for (Map.Entry<String, MutableLong> entry : warningMessages.entrySet()) {
                    message.append("\n * ");
                    message.append(entry.getKey());
                    message.append(" (");
                    message.append(entry.getValue().value);
                    message.append(" times)");
                }
                logger.warn(message.toString());
            }
        }
    }
}
