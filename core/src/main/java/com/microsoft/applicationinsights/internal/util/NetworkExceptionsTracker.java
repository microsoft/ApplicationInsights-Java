package com.microsoft.applicationinsights.internal.util;

import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.failureCounter;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.previousFailureCounter;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.previousSuccessCounter;
import static com.microsoft.applicationinsights.internal.channel.common.TransmissionNetworkOutput.successCounter;

public class NetworkExceptionsTracker implements Runnable{
    @Override public void run() {
        if(failureCounter.get() > 0) {
            previousFailureCounter.set(failureCounter.getAndSet(0));
            previousSuccessCounter.set(successCounter.getAndSet(0));
        }
    }
}
