package com.microsoft.applicationinsights.internal.util;


import static com.microsoft.applicationinsights.common.CommonUtils.failureCounter;
import static com.microsoft.applicationinsights.common.CommonUtils.firstFailure;
import static com.microsoft.applicationinsights.common.CommonUtils.lastTemporaryException;
import static com.microsoft.applicationinsights.common.CommonUtils.lastTemporaryExceptionLogger;
import static com.microsoft.applicationinsights.common.CommonUtils.lastTemporaryExceptionMessage;
import static com.microsoft.applicationinsights.common.CommonUtils.successCounter;

public class NetworkExceptionsTracker implements Runnable{
    @Override public void run() {

        if(failureCounter.get() > 0 && lastTemporaryExceptionLogger.get()!=null) {
            if(!firstFailure.getAndSet(true)) {
                lastTemporaryExceptionLogger.get().error(lastTemporaryExceptionMessage.get()+"\n"+
                                "Total number of successful telemetry requests so far:"+successCounter.get()+"\n"+
                                "Future failures will be aggregated and logged once every 5 minutes\n",
                        lastTemporaryException.get()
                );
            } else {
                lastTemporaryExceptionLogger.get().error(lastTemporaryExceptionMessage.get()+"\n"+
                        "Total number of failed telemetry requests in the last 5 minutes:"+failureCounter.get()+"\n"+
                        "Total number of successful telemetry requests in the last 5 minutes:"+successCounter.get()+"\n"+
                        lastTemporaryException.get()
                );
            }
            failureCounter.set(0);
            successCounter.set(0);
        }
    }
}
