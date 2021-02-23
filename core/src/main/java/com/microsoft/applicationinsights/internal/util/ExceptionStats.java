package com.microsoft.applicationinsights.internal.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

// exception stats for a given 5-min window
public class ExceptionStats {

    private static final ScheduledExecutorService scheduledExecutor =
            Executors.newSingleThreadScheduledExecutor(ThreadPoolUtils.createDaemonThreadFactory(ExceptionStats.class, "exception stats logger"));

    private final AtomicBoolean firstFailure = new AtomicBoolean(false);

    // number of successes and failures in the 5-min window
    private long numSuccesses;
    private long numFailures;

    // these represent the first exception recorded in the 5-min window
    private String warningMessage;
    private Exception exception;
    private Logger logger;
    // Initial Delay for scheduled executor in milliseconds
    private int executorInitialDelay;
    // Period for scheduled executor in milliseconds
    private int executorPeriod;

    private final Object lock = new Object();
    // Primarily used by test
    public ExceptionStats(int executorInitialDelay, int executorPeriod) {
        this.executorInitialDelay = executorInitialDelay;
        this.executorPeriod = executorPeriod;
    }

    public ExceptionStats() {
        this.executorInitialDelay = 300;
        this.executorPeriod = 300;
    }

    public void recordSuccess() {
        numSuccesses++;
    }

    public void recordException(String warningMessage, Exception exception, Logger logger) {
        // log the first exception as soon as it occurs, then log only every 5 min after that
        if (!firstFailure.getAndSet(true)) {
            logger.warn(warningMessage + " (future failures will be aggregated and logged once every 5 minutes)", exception);
            scheduledExecutor.scheduleAtFixedRate(new ExceptionStatsLogger(), executorInitialDelay, executorPeriod, TimeUnit.SECONDS);
            return;
        }

        synchronized (lock) {
            if (numFailures == 0) {
                this.warningMessage = warningMessage;
                this.exception = exception;
                this.logger = logger;
            }
            numFailures++;
        }
    }

    public class ExceptionStatsLogger implements Runnable {

        @Override
        public void run() {
            long numSuccesses;
            long numFailures;
            String warningMessage;
            Exception exception;
            Logger logger;
            // grab quickly and reset under lock (do not perform logging under lock)
            synchronized (lock) {
                numSuccesses = ExceptionStats.this.numSuccesses;
                numFailures = ExceptionStats.this.numFailures;
                warningMessage = ExceptionStats.this.warningMessage;
                exception = ExceptionStats.this.exception;
                logger = ExceptionStats.this.logger;

                ExceptionStats.this.numSuccesses = 0;
                ExceptionStats.this.numFailures = 0;
                ExceptionStats.this.warningMessage = null;
                ExceptionStats.this.exception = null;
                ExceptionStats.this.logger = null;
            }
            if (numFailures > 0) {
                logger.error(warningMessage + "\n" +
                        "Total number of failed telemetry requests in the last 5 minutes: " + numFailures + "\n" +
                        "Total number of successful telemetry requests in the last 5 minutes: " + numSuccesses + "\n", exception);
            }
        }
    }
}
