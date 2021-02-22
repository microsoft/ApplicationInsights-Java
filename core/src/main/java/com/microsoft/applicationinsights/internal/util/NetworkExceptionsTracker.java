package com.microsoft.applicationinsights.internal.util;


import com.microsoft.applicationinsights.customExceptions.TemporaryException;
import static com.microsoft.applicationinsights.common.CommonUtils.firstFailure;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.temporaryNetworkException;

public class NetworkExceptionsTracker implements Runnable{
    @Override public void run() {
        TemporaryException temporaryException = temporaryNetworkException.get();
        if(temporaryException.getFailureCounter() > 0 && temporaryException.getLastTemporaryExceptionLogger()!=null) {
            if(!firstFailure.getAndSet(true)) {
                temporaryException.getLastTemporaryExceptionLogger().error(temporaryException.getLastTemporaryExceptionMessage()+"\n"+
                                "Total number of successful telemetry requests so far:"+temporaryException.getSuccessCounter()+"\n"+
                                "Future failures will be aggregated and logged once every 5 minutes\n",
                        temporaryException.getLastTemporaryException()
                );
            } else {
                temporaryException.getLastTemporaryExceptionLogger().error(temporaryException.getLastTemporaryExceptionMessage()+"\n"+
                        "Total number of failed telemetry requests in the last 5 minutes:"+temporaryException.getFailureCounter()+"\n"+
                        "Total number of successful telemetry requests in the last 5 minutes:"+temporaryException.getSuccessCounter()+"\n"+
                        temporaryException.getLastTemporaryException()
                );
            }
            temporaryNetworkException.set(new TemporaryException());
        }
    }
}
