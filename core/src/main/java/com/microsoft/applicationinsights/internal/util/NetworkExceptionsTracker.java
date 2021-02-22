package com.microsoft.applicationinsights.internal.util;


import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.firstFailure;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.temporaryNetworkException;

public class NetworkExceptionsTracker implements Runnable{
    @Override public void run() {
        TemporaryExceptionWrapper temporaryExceptionWrapper = temporaryNetworkException.get();
        if(temporaryExceptionWrapper.getFailureCounter() > 0 && temporaryExceptionWrapper.getLastTemporaryExceptionLogger()!=null) {
            if(!firstFailure.getAndSet(true)) {
                temporaryExceptionWrapper.getLastTemporaryExceptionLogger().error(temporaryExceptionWrapper.getLastTemporaryExceptionMessage()+"\n"+
                                "Total number of successful telemetry requests so far:"+ temporaryExceptionWrapper.getSuccessCounter()+"\n"+
                                "Future failures will be aggregated and logged once every 5 minutes\n",
                        temporaryExceptionWrapper.getLastTemporaryException()
                );
            } else {
                temporaryExceptionWrapper.getLastTemporaryExceptionLogger().error(temporaryExceptionWrapper.getLastTemporaryExceptionMessage()+"\n"+
                        "Total number of failed telemetry requests in the last 5 minutes:"+ temporaryExceptionWrapper.getFailureCounter()+"\n"+
                        "Total number of successful telemetry requests in the last 5 minutes:"+ temporaryExceptionWrapper.getSuccessCounter()+"\n"+
                        temporaryExceptionWrapper.getLastTemporaryException()
                );
            }
            temporaryNetworkException.set(new TemporaryExceptionWrapper());
        }
    }
}
